package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.entity.Source;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.mapper.SourceMapper;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.AutoPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline A: auto-publish candidates without admin review.
 * Used for github, tools, arena content types.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPublishServiceImpl implements AutoPublishService {

    private final ContentCandidateMapper candidateMapper;
    private final ContentMapper contentMapper;
    private final CategoryMapper categoryMapper;
    private final SourceMapper sourceMapper;
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
    @Transactional
    public void publishCandidate(Long candidateId) {
        ContentCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null || "imported".equals(candidate.getStatus())) {
            return;
        }
        doPublish(candidate);
    }

    @Override
    @Transactional
    public int publishPending(String sourceType) {
        List<ContentCandidate> pending = candidateMapper.selectList(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getSourceType, sourceType)
                        .eq(ContentCandidate::getStatus, "pending")
                        .orderByDesc(ContentCandidate::getCreatedAt));

        int count = 0;
        for (ContentCandidate candidate : pending) {
            try {
                doPublish(candidate);
                count++;
            } catch (Exception e) {
                log.warn("Auto-publish failed for candidate {}: {}", candidate.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("Auto-published {} {} candidates", count, sourceType);
        }
        return count;
    }

    private void doPublish(ContentCandidate candidate) {
        String sourceType = candidate.getSourceType();
        String contentType = mapContentType(sourceType);
        Long categoryId = findCategoryId(mapCategorySlug(sourceType));
        Long sourceId = findSourceId(mapSourceSlug(sourceType));

        String title = candidate.getTitle();
        String slug = generateSlug(title);

        // Generate full Chinese article via AI
        String aiResult = generateFullArticle(candidate);
        String summary = extractSummary(aiResult);
        String bodyMarkdown = extractBody(aiResult);

        if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
            // Fallback to existing data if AI generation fails
            summary = candidate.getAiSummary();
            bodyMarkdown = candidate.getAiBody();
            if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
                bodyMarkdown = candidate.getRawContent() != null ? candidate.getRawContent() : " ";
            }
        }

        Content content = new Content();
        content.setTitle(title.length() > 200 ? title.substring(0, 197) + "..." : title);
        content.setSlug(slug);
        content.setContentType(contentType);
        content.setSummary(summary);
        content.setBodyMarkdown(bodyMarkdown);
        content.setCategoryId(categoryId);
        content.setSourceId(sourceId);
        content.setSourceUrl(candidate.getUrl());
        content.setAuthorName(candidate.getAuthor());
        content.setPublishStatus("PUBLISHED");
        content.setPublishedAt(candidate.getPublishedAt() != null
                ? candidate.getPublishedAt() : LocalDateTime.now());
        content.setExtraJson(candidate.getMetadataJson());
        content.setTrendScore(candidate.getTrendScore());
        content.setStarGrowth7d(candidate.getStarGrowth7d());
        content.setForkCount(candidate.getForkCount());
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        content.setFeaturedLevel(0);
        content.setViewCount(0);
        content.setReadingTime(0);

        contentMapper.insert(content);

        candidate.setStatus("imported");
        candidate.setContentId(content.getId());
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);
    }

    /**
     * Call AI to generate a full Chinese article (300-500 words)
     */
    private String generateFullArticle(ContentCandidate candidate) {
        try {
            String apiKey = resolveApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("No AI API key configured, skipping article generation");
                return null;
            }

            String baseUrl = resolveBaseUrl();
            String model = resolveModel();

            Map<String, Object> request = buildArticleRequest(candidate, model);
            String rawResponse = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey.trim())
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        setConnectTimeout(10000);
                        setReadTimeout(60000);
                    }})
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            return parseAiResponse(rawResponse);
        } catch (RestClientException e) {
            log.warn("AI service unavailable: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Failed to generate article: {}", e.getMessage());
            return null;
        }
    }

    private String resolveApiKey() {
        // Try Bailian first (more reliable), fall back to MiMo
        String bailianKey = apiCredentialService.resolveBailianApiKey(bailianApiKey);
        if (bailianKey != null && !bailianKey.isBlank()) {
            return bailianKey;
        }
        return apiCredentialService.resolveMimoApiKey(mimoApiKey);
    }

    private String resolveBaseUrl() {
        String bailianKey = apiCredentialService.resolveBailianApiKey(bailianApiKey);
        if (bailianKey != null && !bailianKey.isBlank()) {
            return bailianBaseUrl;
        }
        return mimoBaseUrl;
    }

    private String resolveModel() {
        String bailianKey = apiCredentialService.resolveBailianApiKey(bailianApiKey);
        if (bailianKey != null && !bailianKey.isBlank()) {
            return bailianModel;
        }
        return mimoModel;
    }

    private Map<String, Object> buildArticleRequest(ContentCandidate candidate, String model) {
        String rawContent = candidate.getRawContent() != null ? candidate.getRawContent() : "";
        if (rawContent.length() > 2000) {
            rawContent = rawContent.substring(0, 2000) + "...";
        }

        String prompt = String.format("""
                你是AI前沿情报站的内容编辑。请基于以下信息生成一篇完整的中文介绍文章（300-500字）。

                标题：%s
                来源：%s
                原始内容：%s

                要求：
                1. 必须使用中文输出
                2. 使用结构化Markdown格式，包含：
                   ## 一句话摘要
                   用一句简洁的话概括核心内容。

                   ## 核心要点
                   列出3-5个关键要点。

                   ## 详细内容
                   用中文详细描述，保持专业性和准确性。

                   ## 影响/意义
                   分析该内容对AI行业/技术发展的影响。

                3. 简洁、专业、信息密度高
                4. 不要添加额外评论
                """,
                candidate.getTitle() != null ? candidate.getTitle() : "未知标题",
                candidate.getSourceType() != null ? candidate.getSourceType() : "unknown",
                rawContent
        );

        return Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );
    }

    private String parseAiResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract summary from "## 一句话摘要" section
     */
    private String extractSummary(String aiResult) {
        if (aiResult == null || aiResult.isBlank()) {
            return null;
        }
        try {
            // Find "## 一句话摘要" section
            int summaryStart = aiResult.indexOf("## 一句话摘要");
            if (summaryStart == -1) {
                return null;
            }
            summaryStart = aiResult.indexOf("\n", summaryStart) + 1;
            int summaryEnd = aiResult.indexOf("##", summaryStart);
            if (summaryEnd == -1) {
                summaryEnd = aiResult.length();
            }
            return aiResult.substring(summaryStart, summaryEnd).trim();
        } catch (Exception e) {
            log.warn("Failed to extract summary: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract body (everything after "## 一句话摘要")
     */
    private String extractBody(String aiResult) {
        if (aiResult == null || aiResult.isBlank()) {
            return null;
        }
        try {
            // Return full content as body markdown
            return aiResult.trim();
        } catch (Exception e) {
            log.warn("Failed to extract body: {}", e.getMessage());
            return null;
        }
    }

    private String mapContentType(String sourceType) {
        switch (sourceType) {
            case "github":  return "project";
            case "news":    return "news";
            case "product": return "company_update";
            case "arena":   return "arena";
            case "tools":   return "practice";
            default:        return "news";
        }
    }

    private String mapCategorySlug(String sourceType) {
        switch (sourceType) {
            case "github":  return "github";
            case "news":    return "news";
            case "product": return "products";
            case "arena":   return "arena";
            case "tools":   return "tools";
            default:        return "news";
        }
    }

    private String mapSourceSlug(String sourceType) {
        switch (sourceType) {
            case "github":  return "github-trending";
            case "news":    return "hacker-news";
            case "product": return "openai-blog";
            case "arena":   return "chatbot-arena";
            case "tools":   return "reddit";
            default:        return "github-trending";
        }
    }

    private Long findCategoryId(String slug) {
        Category cat = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getSlug, slug));
        return cat != null ? cat.getId() : null;
    }

    private Long findSourceId(String slug) {
        Source src = sourceMapper.selectOne(
                new LambdaQueryWrapper<Source>().eq(Source::getSlug, slug));
        return src != null ? src.getId() : null;
    }

    private String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        // Truncate to fit VARCHAR(220), leave room for UUID suffix
        if (slug.length() > 200) {
            slug = slug.substring(0, 200).replaceAll("-$", "");
        }
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
