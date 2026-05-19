package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.common.exception.BizException;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.entity.Source;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.mapper.SourceMapper;
import com.harry.aifrontier.entity.ApiCredential;
import com.harry.aifrontier.mapper.ApiCredentialMapper;
import com.harry.aifrontier.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private static final Logger log = LoggerFactory.getLogger(CandidateServiceImpl.class);

    private final ContentCandidateMapper candidateMapper;
    private final ContentMapper contentMapper;
    private final CategoryMapper categoryMapper;
    private final SourceMapper sourceMapper;
    private final GitHubTrendingService gitHubTrendingService;
    private final NewsRssService newsRssService;
    private final ProductUpdateService productUpdateService;
    private final ToolsRssService toolsRssService;
    private final ArenaService arenaService;
    private final ObjectMapper objectMapper;
    private final ApiCredentialService apiCredentialService;
    private final ApiCredentialMapper apiCredentialMapper;
    private final EventService eventService;

    @Value("${app.bailian.api-key:}")
    private String bailianApiKey;

    @Value("${app.bailian.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String bailianBaseUrl;

    @Value("${app.bailian.model:qwen-plus}")
    private String bailianModel;

    // ========================================================================
    // Public API
    // ========================================================================

    @Override
    public Page<ContentCandidate> listCandidates(String sourceType, String status,
                                                  Long pageNum, Long pageSize) {
        LambdaQueryWrapper<ContentCandidate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(ContentCandidate::getSourceType, sourceType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ContentCandidate::getStatus, status);
        }
        wrapper.orderByDesc(ContentCandidate::getCreatedAt);

        return candidateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ContentCandidate getCandidate(Long id) {
        ContentCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw BizException.notFound("候选内容不存在");
        }
        return candidate;
    }

    @Override
    public ContentCandidate updateCandidate(Long id, Map<String, String> updates) {
        ContentCandidate candidate = getCandidate(id);

        if (updates.containsKey("aiSummary")) {
            candidate.setAiSummary(updates.get("aiSummary"));
        }
        if (updates.containsKey("aiBody")) {
            candidate.setAiBody(updates.get("aiBody"));
        }
        if (updates.containsKey("metadataJson")) {
            candidate.setMetadataJson(updates.get("metadataJson"));
        }
        if (updates.containsKey("title")) {
            candidate.setTitle(updates.get("title"));
        }

        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);
        return candidate;
    }

    @Override
    public Long approveCandidate(Long id, Map<String, String> overrides) {
        ContentCandidate candidate = getCandidate(id);
        if ("imported".equals(candidate.getStatus())) {
            throw BizException.badRequest("该候选内容已导入");
        }

        // Determine values (with optional overrides)
        String title = (overrides != null && overrides.containsKey("title"))
                ? overrides.get("title") : candidate.getTitle();
        String summary = (overrides != null && overrides.containsKey("summary"))
                ? overrides.get("summary") : candidate.getAiSummary();
        String bodyMarkdown = (overrides != null && overrides.containsKey("bodyMarkdown"))
                ? overrides.get("bodyMarkdown") : candidate.getAiBody();
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
            bodyMarkdown = candidate.getRawContent() != null ? candidate.getRawContent() : " ";
        }

        // Map sourceType to contentType, categoryId, sourceId (DB lookup!)
        String sourceType = candidate.getSourceType();
        String contentType = mapContentType(sourceType);
        Long categoryId = findCategoryId(mapCategorySlug(sourceType));
        Long sourceId = findSourceId(mapSourceSlug(sourceType));

        // Generate slug
        String slug = generateSlug(title);

        // Create Content record
        Content content = new Content();
        content.setTitle(title);
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

        // Update candidate status
        candidate.setStatus("imported");
        candidate.setContentId(content.getId());
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);

        return content.getId();
    }

    @Override
    public void rejectCandidate(Long id) {
        ContentCandidate candidate = getCandidate(id);
        if ("imported".equals(candidate.getStatus())) {
            throw BizException.badRequest("已导入的候选内容不可驳回");
        }

        candidate.setStatus("rejected");
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);
    }

    @Override
    public ContentCandidate aiProcess(Long id, String customPrompt) {
        ContentCandidate candidate = getCandidate(id);

        // Build prompt
        String prompt;
        if (customPrompt != null && !customPrompt.isBlank()) {
            String rawContent = valueOrDefault(candidate.getRawContent(), "");
            String title = valueOrDefault(candidate.getTitle(), "");
            prompt = customPrompt
                    + "\n\n---\n以下是需要处理的原始内容：\n"
                    + "标题：" + title + "\n"
                    + "内容：" + rawContent;
        } else {
            prompt = buildAiPrompt(candidate);
        }

        // Call AI API
        String rawResponse;
        try {
            String apiKey = apiCredentialService.resolveBailianApiKey(bailianApiKey);
            if (apiKey == null || apiKey.isBlank()) {
                throw BizException.serverError("未配置百炼 API Key，请先在设置中配置");
            }

            rawResponse = RestClient.builder()
                    .baseUrl(bailianBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey.trim())
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildChatRequest(prompt, bailianModel))
                    .retrieve()
                    .body(String.class);
        } catch (BizException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw BizException.serverError("AI 服务暂时无法访问，请检查网络、API Key 或稍后重试");
        }

        // Parse AI response and update candidate
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            String jsonContent = content.replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();
            JsonNode payload = objectMapper.readTree(jsonContent);

            String summary = payload.path("summary").asText("");
            String bodyMarkdown = payload.path("body_markdown").asText("");
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = payload.path("tags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(t -> {
                    String v = t.asText();
                    if (v != null && !v.isBlank()) tags.add(v.trim());
                });
            }
            String direction = payload.path("direction").asText("");
            String difficulty = payload.path("difficulty").asText("");
            String audience = payload.path("audience").asText("");
            String learningPath = payload.path("learning_path").asText("");

            // Update candidate
            candidate.setAiSummary(summary);
            candidate.setAiBody(bodyMarkdown);

            // Store extra AI metadata in metadataJson
            Map<String, Object> extraMeta = new LinkedHashMap<>();
            String existingMeta = candidate.getMetadataJson();
            if (existingMeta != null && !existingMeta.isBlank()) {
                try {
                    JsonNode existing = objectMapper.readTree(existingMeta);
                    existing.fields().forEachRemaining(e -> extraMeta.put(e.getKey(), e.getValue()));
                } catch (Exception ignored) {
                }
            }
            extraMeta.put("aiTags", tags);
            extraMeta.put("aiDirection", direction);
            extraMeta.put("aiDifficulty", difficulty);
            extraMeta.put("aiAudience", audience);
            extraMeta.put("aiLearningPath", learningPath);
            extraMeta.put("aiProcessedAt", LocalDateTime.now().toString());
            candidate.setMetadataJson(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(extraMeta));

            candidate.setUpdatedAt(LocalDateTime.now());
            candidateMapper.updateById(candidate);

            return candidate;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI response parse error: {}", ex.getMessage(), ex);
            throw BizException.serverError("AI 返回内容解析失败，请稍后重试");
        }
    }

    @Override
    public int fetchFromSource(String sourceType) {
        int count;
        switch (sourceType) {
            case "github":
                count = gitHubTrendingService.fetchTrending("", "daily");
                break;
            case "news":
                count = newsRssService.fetchNews();
                break;
            case "product":
                count = productUpdateService.fetchProductUpdates();
                break;
            case "tools":
                count = toolsRssService.fetchTools();
                break;
            case "arena":
                count = arenaService.fetchArenaData();
                break;
            default:
                throw BizException.badRequest("暂不支持的数据源类型: " + sourceType);
        }

        // Auto-cluster after fetch
        try {
            int events = eventService.autoCluster();
            log.info("Auto-cluster after fetch {}: {} events created", sourceType, events);
        } catch (Exception e) {
            log.warn("Auto-cluster after fetch failed: {}", e.getMessage());
        }

        return count;
    }

    // ========================================================================
    // Content type & slug mapping (replaces hardcoded IDs)
    // ========================================================================

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

    /**
     * Map sourceType to the category slug stored in DB.
     */
    private String mapCategorySlug(String sourceType) {
        switch (sourceType) {
            case "github":  return "products";
            case "news":    return "news";
            case "product": return "products";
            case "arena":   return "arena";
            case "tools":   return "tools";
            default:        return "news";
        }
    }

    /**
     * Map sourceType to the source slug stored in DB.
     */
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

    /**
     * Look up category ID by slug from DB (replaces hardcoded IDs).
     */
    private Long findCategoryId(String slug) {
        Category cat = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getSlug, slug));
        return cat != null ? cat.getId() : null;
    }

    /**
     * Look up source ID by slug from DB (replaces hardcoded IDs).
     */
    private Long findSourceId(String slug) {
        Source src = sourceMapper.selectOne(
                new LambdaQueryWrapper<Source>().eq(Source::getSlug, slug));
        return src != null ? src.getId() : null;
    }

    // ========================================================================
    // Slug generation
    // ========================================================================

    private String generateSlug(String title) {
        if (!StringUtils.hasText(title)) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    // ========================================================================
    // AI processing helpers
    // ========================================================================

    private String buildAiPrompt(ContentCandidate candidate) {
        String sourceType = candidate.getSourceType();
        if ("github".equals(sourceType)) {
            return buildGitHubPrompt(candidate);
        }
        return buildGenericPrompt(candidate);
    }

    private String buildGitHubPrompt(ContentCandidate candidate) {
        String meta = candidate.getMetadataJson();
        String stars = "", forks = "", language = "", topics = "";
        if (meta != null && !meta.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(meta);
                stars = node.path("stars").asText("");
                forks = node.path("forks").asText("");
                language = node.path("language").asText("");
                JsonNode topicsNode = node.path("topics");
                if (topicsNode.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    topicsNode.forEach(t -> {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(t.asText());
                    });
                    topics = sb.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return """
                你是AI前沿情报站的内容编辑。请基于以下GitHub项目信息，生成中文内容。

                项目名称：%s
                项目描述：%s
                Star数：%s
                Fork数：%s
                编程语言：%s
                标签：%s

                请严格以JSON格式输出，不要添加其他内容：
                {
                  "summary": "一句话中文摘要（50字内，突出项目核心价值）",
                  "body_markdown": "详细中文介绍（300-500字），包含：\\n## 项目简介\\n...\\n## 核心功能\\n...\\n## 技术实现\\n...\\n## 适用场景\\n...",
                  "tags": ["标签1", "标签2", "标签3"],
                  "direction": "llm/agent/diffusion/cv/nlp/infra/tool",
                  "difficulty": "beginner/intermediate/advanced",
                  "audience": "适合xxx开发者/研究者",
                  "learning_path": "建议先学xxx再看这个"
                }
                """.formatted(
                valueOrDefault(candidate.getTitle(), "未知"),
                valueOrDefault(candidate.getRawContent(), "无描述"),
                valueOrDefault(stars, "未知"),
                valueOrDefault(forks, "未知"),
                valueOrDefault(language, "未知"),
                valueOrDefault(topics, "无")
        );
    }

    private String buildGenericPrompt(ContentCandidate candidate) {
        String sourceType = candidate.getSourceType();
        String sourceLabel;
        switch (sourceType) {
            case "news":    sourceLabel = "资讯"; break;
            case "tools":   sourceLabel = "工具"; break;
            case "product": sourceLabel = "产品"; break;
            case "arena":   sourceLabel = "评测"; break;
            default:        sourceLabel = sourceType;
        }
        return """
                你是AI前沿情报站的内容编辑。请基于以下信息，生成中文内容。

                标题：%s
                内容：%s
                来源：%s

                请严格以JSON格式输出：
                {
                  "summary": "一句话中文摘要（50字内）",
                  "body_markdown": "详细中文介绍（200-400字）",
                  "tags": ["标签1", "标签2"],
                  "direction": "industry/policy/research/product/tool",
                  "difficulty": "beginner/intermediate/advanced",
                  "audience": "适合xxx",
                  "learning_path": "建议xxx"
                }
                """.formatted(
                valueOrDefault(candidate.getTitle(), "未知"),
                valueOrDefault(candidate.getRawContent(), "无内容"),
                sourceLabel
        );
    }

    private Map<String, Object> buildChatRequest(String prompt, String model) {
        return Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "你是 AI 前沿情报站的内容编辑助手。请严格按要求输出 JSON，不要使用 Markdown 代码块。"),
                        Map.of("role", "user", "content", prompt)
                )
        );
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    // ========================================================================
    // Custom prompt persistence
    // ========================================================================

    private static final String PROMPT_PROVIDER = "custom_prompt";

    @Override
    public String getCustomPrompt() {
        ApiCredential record = apiCredentialMapper.selectOne(
                new LambdaQueryWrapper<ApiCredential>()
                        .eq(ApiCredential::getProvider, PROMPT_PROVIDER)
        );
        return record != null ? record.getEncryptedKey() : "";
    }

    @Override
    public void saveCustomPrompt(String prompt) {
        ApiCredential existing = apiCredentialMapper.selectOne(
                new LambdaQueryWrapper<ApiCredential>()
                        .eq(ApiCredential::getProvider, PROMPT_PROVIDER)
        );
        if (existing != null) {
            existing.setEncryptedKey(prompt != null ? prompt : "");
            existing.setUpdatedAt(LocalDateTime.now());
            apiCredentialMapper.updateById(existing);
        } else {
            ApiCredential record = new ApiCredential();
            record.setProvider(PROMPT_PROVIDER);
            record.setEncryptedKey(prompt != null ? prompt : "");
            record.setIv("plaintext");
            record.setKeySuffix("prompt");
            record.setRemark("自定义AI提示词");
            record.setIsEnabled(1);
            apiCredentialMapper.insert(record);
        }
    }

    @Override
    public String translateContent(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) return "内容不存在";

        String body = content.getBodyMarkdown();
        if (body == null || body.isBlank()) return "无正文内容";

        // Check if already Chinese (skip if >50% Chinese chars)
        long chineseChars = body.chars().filter(c -> c >= 0x4e00 && c <= 0x9fff).count();
        if (chineseChars > body.length() * 0.3) {
            return "内容已是中文，跳过翻译";
        }

        String apiKey = apiCredentialService.resolveBailianApiKey(bailianApiKey);
        if (apiKey == null || apiKey.isBlank()) {
            return "百炼 API Key 未配置";
        }

        String prompt = "请将以下AI领域内容翻译并整理为中文，使用结构化Markdown格式。\n\n" +
                "标题：" + content.getTitle() + "\n\n" +
                "原始内容：\n" + body.substring(0, Math.min(body.length(), 2000)) + "\n\n" +
                "要求：\n" +
                "1. 必须使用中文输出\n" +
                "2. 使用结构化Markdown，包含：## 核心要点（3-5条）和 ## 详细内容\n" +
                "3. 保持专业性和准确性\n" +
                "4. 不要添加额外评论";

        try {
            String translated = callAiApi(prompt, apiKey);
            if (translated != null && !translated.isBlank()) {
                content.setBodyMarkdown(translated);
                contentMapper.updateById(content);
                log.info("Translated content {}: {} chars", contentId, translated.length());
                return "翻译完成";
            }
            return "AI返回为空";
        } catch (Exception e) {
            log.error("Translation failed for content {}: {}", contentId, e.getMessage());
            return "翻译失败: " + e.getMessage();
        }
    }

    @Override
    public int batchTranslateEnglish() {
        List<Content> englishContents = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .isNotNull(Content::getBodyMarkdown)
                        .ne(Content::getBodyMarkdown, " ")
                        .ne(Content::getContentType, "paper")
        );

        int processed = 0;
        for (Content content : englishContents) {
            String body = content.getBodyMarkdown();
            if (body == null || body.isBlank()) continue;

            // Skip if already Chinese
            long chineseChars = body.chars().filter(c -> c >= 0x4e00 && c <= 0x9fff).count();
            if (chineseChars > body.length() * 0.3) continue;

            String result = translateContent(content.getId());
            if (result.contains("完成")) {
                processed++;
            }

            // Rate limit: wait 1 second between API calls
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        log.info("Batch translate completed: {}/{} items processed", processed, englishContents.size());
        return processed;
    }

    private String callAiApi(String prompt, String apiKey) {
        java.util.Map<String, Object> body = java.util.Map.of(
                "model", bailianModel,
                "messages", java.util.List.of(
                        java.util.Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "max_tokens", 2000
        );

        String response = RestClient.create(bailianBaseUrl)
                .post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            return null;
        }
    }
}
