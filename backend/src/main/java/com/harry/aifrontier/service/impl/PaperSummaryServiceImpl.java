package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.PaperSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperSummaryServiceImpl implements PaperSummaryService {

    private final ObjectMapper objectMapper;
    private final ApiCredentialService apiCredentialService;
    private final ContentMapper contentMapper;

    @Value("${app.mimo.api-key:}")
    private String mimoApiKey;

    @Value("${app.mimo.base-url:https://token-plan-cn.xiaomimimo.com/v1}")
    private String mimoBaseUrl;

    @Value("${app.mimo.model:mimo-v2.5-pro}")
    private String mimoModel;

    private static final String SYSTEM_PROMPT = """
            你是 AI 前沿情报站的内容编辑，面向大二本科生水平。
            请用中文总结下面这篇论文。要求：
            1. 用中文撰写，仅保留不可替代的英文专有名词（如 CT、diffusion model、U-Net、arXiv 等）
            2. 输出纯文本，不要 JSON，不要 Markdown 代码块
            3. 包含以下部分：
               - 一句话核心贡献
               - 方法简述（3-5句话）
               - 与 CT 去噪/医学影像的关联度（1-10 分）
               - 推荐理由（为什么值得读）
            """;

    @Override
    public String generateChineseSummary(String title, String abstractText) {
        String apiKey = apiCredentialService.resolveMimoApiKey(mimoApiKey);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MiMo API Key 未配置，无法生成中文摘要");
        }
        if (abstractText == null || abstractText.trim().length() < 20) {
            throw new IllegalArgumentException("论文摘要过短，无法生成中文摘要");
        }

        String userPrompt = "论文标题：%s\n\n论文摘要：\n%s".formatted(
                title != null ? title : "未填写",
                abstractText.trim()
        );

        Map<String, Object> requestBody = Map.of(
                "model", mimoModel,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        String rawResponse;
        try {
            rawResponse = RestClient.builder()
                    .baseUrl(mimoBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey.trim())
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new RuntimeException("MiMo 服务暂时无法访问，请检查网络或稍后重试", ex);
        }

        return parseContent(rawResponse);
    }

    @Override
    public String generateAllMissing() {
        List<Content> allPublished = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getPublishStatus, "PUBLISHED")
                        .orderByAsc(Content::getId)
        );

        int total = allPublished.size();
        int processed = 0;
        int skipped = 0;
        int success = 0;
        int failed = 0;

        log.info("[批量中文摘要] 开始处理，共 {} 篇已发布内容", total);

        for (Content content : allPublished) {
            processed++;

            // 跳过已有中文摘要的（summary 包含中文字符且长度 > 50 视为已有中文摘要）
            if (hasChineseSummary(content.getSummary())) {
                skipped++;
                log.debug("[批量中文摘要] [{}/{}] 跳过（已有中文摘要）: {}", processed, total, content.getTitle());
                continue;
            }

            String abstractText = content.getSummary();
            if (abstractText == null || abstractText.trim().length() < 20) {
                // 尝试从 bodyMarkdown 提取摘要
                abstractText = extractAbstractFromBody(content.getBodyMarkdown());
            }
            if (abstractText == null || abstractText.trim().length() < 20) {
                skipped++;
                log.debug("[批量中文摘要] [{}/{}] 跳过（无可用摘要）: {}", processed, total, content.getTitle());
                continue;
            }

            try {
                String chineseSummary = generateChineseSummary(content.getTitle(), abstractText);
                content.setSummary(chineseSummary);
                contentMapper.updateById(content);
                success++;
                log.info("[批量中文摘要] [{}/{}] 成功: {}", processed, total, content.getTitle());
            } catch (Exception e) {
                failed++;
                log.warn("[批量中文摘要] [{}/{}] 失败: {} - {}", processed, total, content.getTitle(), e.getMessage());
            }

            // 每篇间隔 2 秒避免 API 限流
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[批量中文摘要] 线程被中断，提前结束");
                break;
            }
        }

        String result = String.format("批量中文摘要完成：共 %d 篇，成功 %d 篇，跳过 %d 篇，失败 %d 篇",
                total, success, skipped, failed);
        log.info("[批量中文摘要] {}", result);
        return result;
    }

    private String parseContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("AI 返回内容为空");
            }
            // 清理可能的 Markdown 代码块包裹
            return content.trim()
                    .replaceAll("^```\\w*\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();
        } catch (Exception ex) {
            throw new RuntimeException("AI 返回内容解析失败", ex);
        }
    }

    /**
     * 判断是否已有中文摘要：包含足够多的中文字符
     */
    private boolean hasChineseSummary(String summary) {
        if (summary == null || summary.length() < 50) {
            return false;
        }
        long chineseCount = summary.chars()
                .filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF)
                .count();
        // 中文字符超过 10 个认为已有中文摘要
        return chineseCount > 10;
    }

    /**
     * 从 bodyMarkdown 中提取 "## 论文摘要" 部分作为摘要
     */
    private String extractAbstractFromBody(String bodyMarkdown) {
        if (bodyMarkdown == null) {
            return null;
        }
        String marker = "## 论文摘要";
        int idx = bodyMarkdown.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        String after = bodyMarkdown.substring(idx + marker.length()).trim();
        int nextHeading = after.indexOf("\n## ");
        if (nextHeading > 0) {
            return after.substring(0, nextHeading).trim();
        }
        return after.trim();
    }
}
