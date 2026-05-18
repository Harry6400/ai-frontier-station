package com.harry.aifrontier.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.dto.request.AiSourceSummaryRequest;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.AiSummaryService;
import com.harry.aifrontier.vo.AiSourceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private final ObjectMapper objectMapper;
    private final ApiCredentialService apiCredentialService;

    @Value("${app.bailian.api-key:}")
    private String bailianApiKey;

    @Value("${app.bailian.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String bailianBaseUrl;

    @Value("${app.bailian.model:qwen-plus}")
    private String bailianModel;

    @Value("${app.mimo.api-key:}")
    private String mimoApiKey;

    @Value("${app.mimo.base-url:https://token-plan-cn.xiaomimimo.com/v1}")
    private String mimoBaseUrl;

    @Value("${app.mimo.model:mimo-v2.5-pro}")
    private String mimoModel;

    private static final String PROVIDER_MIMO = "mimo";

    @Override
    public AiSourceSummaryVO summarizeSource(AiSourceSummaryRequest request) {
        String provider = resolveProvider(request.getProvider());
        String resolvedApiKey;
        String displayName;
        if (PROVIDER_MIMO.equals(provider)) {
            resolvedApiKey = apiCredentialService.resolveMimoApiKey(mimoApiKey);
            displayName = "MiMo";
        } else {
            resolvedApiKey = apiCredentialService.resolveBailianApiKey(bailianApiKey);
            displayName = "百炼";
        }
        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            throw new IllegalArgumentException("未启用 AI 总结能力，请先配置对应 Provider 的 API Key");
        }
        if (request.getOriginalSummary() == null || request.getOriginalSummary().trim().length() < 20) {
            throw new IllegalArgumentException("原文摘要信息过短，请至少填写 20 个字，方便 AI 生成可靠导读");
        }

        String providerBaseUrl = PROVIDER_MIMO.equals(provider) ? mimoBaseUrl : bailianBaseUrl;
        String providerModel = PROVIDER_MIMO.equals(provider) ? mimoModel : bailianModel;
        String rawResponse;
        try {
            rawResponse = RestClient.builder()
                    .baseUrl(providerBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + resolvedApiKey.trim())
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildChatRequest(request, providerModel))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException(displayName + " 服务暂时无法访问，请检查网络、API Key 或稍后重试");
        }

        return parseSummaryResponse(request, rawResponse, provider);
    }

    private String resolveProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return ApiCredentialService.PROVIDER_BAILIAN;
        }
        String trimmed = provider.trim().toLowerCase();
        if (PROVIDER_MIMO.equals(trimmed)) {
            return PROVIDER_MIMO;
        }
        return ApiCredentialService.PROVIDER_BAILIAN;
    }

    private Map<String, Object> buildChatRequest(AiSourceSummaryRequest request, String providerModel) {
        return Map.of(
                "model", providerModel,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", """
                                你是 AI 信息聚合平台的内容编辑助手。请基于用户给出的来源信息生成结构化导读，不要编造来源中没有的信息。
                                输出必须是严格 JSON，不要使用 Markdown 代码块。字段包括：
                                suggestedTitle, summary, sourceBrief, aiSummary, recommendationReason, importanceScore, tagSuggestions, readingTime, bodyMarkdown。
                                importanceScore 是 1 到 100 的整数；tagSuggestions 是 3 到 6 个中文或英文标签。
                                bodyMarkdown 要包含“AI 导读”“来源说明”“原文摘要/摘录”“为什么值得关注”“原始链接”几个小节。
                                """),
                        Map.of("role", "user", "content", buildPrompt(request))
                )
        );
    }

    private String buildPrompt(AiSourceSummaryRequest request) {
        return """
                请整理下面这个 AI 信息来源，用于“AI前沿情报站”的后台人工确认。

                来源标题：%s
                来源链接：%s
                来源名称：%s
                来源类型：%s
                内容类型：%s
                原文摘要：%s
                原文摘录：%s
                图片链接：%s
                编辑要求：%s

                要求：
                1. 不要全文转载原文，只总结和引用必要摘录。
                2. 标题要像内容平台标题，不要像数据库字段。
                3. 推荐理由要说明为什么 AI 开发者或学习者值得看。
                4. 如果信息不足，要明确写“根据当前录入信息判断”。
                """.formatted(
                valueOrDefault(request.getSourceTitle(), "未填写"),
                valueOrDefault(request.getSourceUrl(), "未填写"),
                valueOrDefault(request.getSourceName(), "未填写"),
                valueOrDefault(request.getSourceType(), "manual"),
                valueOrDefault(request.getContentType(), "news"),
                valueOrDefault(request.getOriginalSummary(), "未填写"),
                valueOrDefault(request.getOriginalExcerpt(), "未填写"),
                valueOrDefault(request.getImageUrl(), "未填写"),
                valueOrDefault(request.getInstruction(), "按 AI 信息平台风格生成简洁、可信、可展示的导读")
        );
    }

    private AiSourceSummaryVO parseSummaryResponse(AiSourceSummaryRequest request, String rawResponse, String provider) {
        String displayName = PROVIDER_MIMO.equals(provider) ? "MiMo" : "百炼";
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode payload = objectMapper.readTree(stripJsonFence(content));
            AiSourceSummaryVO vo = new AiSourceSummaryVO();
            vo.setSuggestedTitle(text(payload, "suggestedTitle", request.getSourceTitle()));
            vo.setSummary(text(payload, "summary", request.getOriginalSummary()));
            vo.setSourceBrief(text(payload, "sourceBrief", "该来源由后台人工录入，并由" + displayName + "生成结构化导读。"));
            vo.setAiSummary(text(payload, "aiSummary", vo.getSummary()));
            vo.setRecommendationReason(text(payload, "recommendationReason", "根据当前录入信息判断，该内容适合作为 AI 前沿动态收录。"));
            vo.setImportanceScore(clamp(payload.path("importanceScore").asInt(60), 1, 100));
            vo.setTagSuggestions(parseTags(payload.path("tagSuggestions")));
            vo.setReadingTime(Math.max(3, payload.path("readingTime").asInt(5)));
            vo.setBodyMarkdown(text(payload, "bodyMarkdown", buildFallbackBody(request, vo)));
            vo.setExtraJson(buildExtraJson(request, vo, provider));
            return vo;
        } catch (Exception ex) {
            throw new IllegalArgumentException(displayName + " 返回内容解析失败，请稍后重试或检查输入信息是否过短");
        }
    }

    private String buildFallbackBody(AiSourceSummaryRequest request, AiSourceSummaryVO vo) {
        return """
                ## AI 导读

                %s

                ## 来源说明

                %s

                ## 原文摘要/摘录

                %s

                ## 为什么值得关注

                %s

                ## 原始链接

                [%s](%s)
                """.formatted(
                valueOrDefault(vo.getAiSummary(), vo.getSummary()),
                valueOrDefault(vo.getSourceBrief(), "该来源由后台人工录入。"),
                valueOrDefault(request.getOriginalExcerpt(), request.getOriginalSummary()),
                valueOrDefault(vo.getRecommendationReason(), "该内容适合作为 AI 前沿动态收录。"),
                valueOrDefault(request.getSourceTitle(), request.getSourceUrl()),
                request.getSourceUrl()
        ).trim();
    }

    private String buildExtraJson(AiSourceSummaryRequest request, AiSourceSummaryVO vo, String provider) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("externalType", resolveExternalType(request.getSourceType()));
        extra.put("aiSummary", vo.getAiSummary());
        extra.put("recommendationReason", vo.getRecommendationReason());
        extra.put("importanceScore", vo.getImportanceScore());
        extra.put("tagSuggestions", vo.getTagSuggestions());
        extra.put("sourceBrief", vo.getSourceBrief());
        extra.put("sourceTitle", request.getSourceTitle());
        extra.put("sourceName", valueOrDefault(request.getSourceName(), "人工录入来源"));
        extra.put("sourceType", valueOrDefault(request.getSourceType(), "manual"));
        extra.put("sourceImage", blankToNull(request.getImageUrl()));
        extra.put("originalSummary", blankToNull(request.getOriginalSummary()));
        extra.put("originalExcerpt", blankToNull(request.getOriginalExcerpt()));
        extra.put("aiProvider", PROVIDER_MIMO.equals(provider) ? "MiMo" : "Alibaba Bailian / DashScope");
        extra.put("importMode", "manual_source_ai_summary");
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extra);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("AI 扩展字段生成失败");
        }
    }

    private List<String> parseTags(JsonNode node) {
        List<String> tags = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String value = item.asText();
                if (value != null && !value.isBlank()) {
                    tags.add(value.trim());
                }
            });
        }
        if (tags.isEmpty()) {
            tags.addAll(List.of("AI动态", "来源精选", "百炼总结"));
        }
        return tags.stream().distinct().limit(6).toList();
    }

    private String stripJsonFence(String content) {
        if (content == null) {
            return "{}";
        }
        return content.replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();
    }

    private String resolveExternalType(String sourceType) {
        if ("github".equals(sourceType)) {
            return "github_repo";
        }
        if ("paper".equals(sourceType)) {
            return "arxiv_paper";
        }
        if ("official_blog".equals(sourceType)) {
            return "official_post";
        }
        if ("community".equals(sourceType)) {
            return "community_practice";
        }
        if ("leaderboard".equals(sourceType)) {
            return "leaderboard_item";
        }
        return "manual_source";
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            return valueOrDefault(fallback, "");
        }
        return value.trim();
    }

    private Integer clamp(Integer value, Integer min, Integer max) {
        return Math.max(min, Math.min(max, value));
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
