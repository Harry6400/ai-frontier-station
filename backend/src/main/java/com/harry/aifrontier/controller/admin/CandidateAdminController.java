package com.harry.aifrontier.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.GitHubTrendingService;
import com.harry.aifrontier.service.NewsRssService;
import com.harry.aifrontier.service.ProductUpdateService;
import com.harry.aifrontier.service.ToolsRssService;
import com.harry.aifrontier.service.ArenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/candidates")
@RequiredArgsConstructor
public class CandidateAdminController {

    private final ContentCandidateMapper candidateMapper;
    private final ContentMapper contentMapper;
    private final GitHubTrendingService gitHubTrendingService;
    private final NewsRssService newsRssService;
    private final ProductUpdateService productUpdateService;
    private final ToolsRssService toolsRssService;
    private final ArenaService arenaService;
    private final ObjectMapper objectMapper;
    private final ApiCredentialService apiCredentialService;

    @Value("${app.bailian.api-key:}")
    private String bailianApiKey;

    @Value("${app.bailian.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String bailianBaseUrl;

    @Value("${app.bailian.model:qwen-plus}")
    private String bailianModel;

    /**
     * List candidates with optional filters
     */
    @GetMapping
    public ApiResponse<PageResult<ContentCandidate>> list(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false, defaultValue = "pending") String status,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {

        LambdaQueryWrapper<ContentCandidate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(ContentCandidate::getSourceType, sourceType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ContentCandidate::getStatus, status);
        }
        wrapper.orderByDesc(ContentCandidate::getCreatedAt);

        Page<ContentCandidate> page = candidateMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        return ApiResponse.success(PageResult.of(
                page.getTotal(), pageNum, pageSize, page.getRecords()));
    }

    /**
     * Get single candidate detail
     */
    @GetMapping("/{id}")
    public ApiResponse<ContentCandidate> detail(@PathVariable Long id) {
        ContentCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            return ApiResponse.fail(404, "候选内容不存在");
        }
        return ApiResponse.success(candidate);
    }

    /**
     * Update candidate fields
     */
    @PutMapping("/{id}")
    public ApiResponse<ContentCandidate> update(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        ContentCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            return ApiResponse.fail(404, "候选内容不存在");
        }

        if (body.containsKey("aiSummary")) {
            candidate.setAiSummary(body.get("aiSummary"));
        }
        if (body.containsKey("aiBody")) {
            candidate.setAiBody(body.get("aiBody"));
        }
        if (body.containsKey("metadataJson")) {
            candidate.setMetadataJson(body.get("metadataJson"));
        }
        if (body.containsKey("title")) {
            candidate.setTitle(body.get("title"));
        }

        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);
        return ApiResponse.success("候选内容更新成功", candidate);
    }

    /**
     * Approve and import candidate into Content table
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Long> approve(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> overrides) {
        ContentCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            return ApiResponse.fail(404, "候选内容不存在");
        }
        if ("imported".equals(candidate.getStatus())) {
            return ApiResponse.fail(400, "该候选内容已导入");
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

        // Map sourceType to contentType, categoryId, sourceId
        String sourceType = candidate.getSourceType();
        String contentType = mapContentType(sourceType);
        Long categoryId = mapCategoryId(sourceType);
        Long sourceId = mapSourceId(sourceType);

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

        return ApiResponse.success("审批导入成功", content.getId());
    }

    /**
     * Reject candidate
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        ContentCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            return ApiResponse.fail(404, "候选内容不存在");
        }
        if ("imported".equals(candidate.getStatus())) {
            return ApiResponse.fail(400, "已导入的候选内容不可驳回");
        }

        candidate.setStatus("rejected");
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);

        return ApiResponse.success("已驳回", null);
    }

    /**
     * AI-process a candidate: send to AI API and generate structured content
     */
    @PostMapping("/{id}/ai-process")
    public ApiResponse<ContentCandidate> aiProcess(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, String> body) {
        ContentCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            return ApiResponse.fail(404, "候选内容不存在");
        }

        // Build prompt: use custom prompt from request body if provided, otherwise default
        String prompt;
        String customPrompt = (body != null) ? body.get("prompt") : null;
        if (customPrompt != null && !customPrompt.isBlank()) {
            // Append candidate's raw content to the custom prompt so the AI has context
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
                return ApiResponse.fail(500, "未配置百炼 API Key，请先在设置中配置");
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
        } catch (RestClientException ex) {
            return ApiResponse.fail(500, "AI 服务暂时无法访问，请检查网络、API Key 或稍后重试");
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
                } catch (Exception ignored) {}
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

            return ApiResponse.success("AI 处理完成", candidate);
        } catch (Exception ex) {
            return ApiResponse.fail(500, "AI 返回内容解析失败，请稍后重试");
        }
    }

    /**
     * Trigger manual fetch for a source type
     */
    @PostMapping("/fetch/{sourceType}")
    public ApiResponse<Integer> fetch(@PathVariable String sourceType) {
        int count;
        switch (sourceType) {
            case "github": count = gitHubTrendingService.fetchTrending("", "daily"); break;
            case "news": count = newsRssService.fetchNews(); break;
            case "product": count = productUpdateService.fetchProductUpdates(); break;
            case "tools": count = toolsRssService.fetchTools(); break;
            case "arena": count = arenaService.fetchArenaData(); break;
            default: return ApiResponse.fail(400, "暂不支持的数据源类型: " + sourceType);
        }
        return ApiResponse.success("获取完成，新增 " + count + " 条候选", count);
    }

    // ---- helper methods ----

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

    private Long mapCategoryId(String sourceType) {
        switch (sourceType) {
            case "github":  return 7L; // 产品动态
            case "news":    return 6L; // AI资讯
            case "product": return 7L; // 产品动态
            case "arena":   return 8L; // 模型评测
            case "tools":   return 9L; // 工具实践
            default:        return 6L;
        }
    }

    private Long mapSourceId(String sourceType) {
        switch (sourceType) {
            case "github":  return 1L;  // GitHub Trending
            case "news":    return 11L; // Hacker News
            case "product": return 3L;  // OpenAI Blog / Product Hunt
            case "arena":   return 8L;  // Chatbot Arena
            case "tools":   return 10L; // Reddit
            default:        return 1L;
        }
    }

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

    // ---- AI processing helpers ----

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
            } catch (Exception ignored) {}
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
            case "news": sourceLabel = "资讯"; break;
            case "tools": sourceLabel = "工具"; break;
            case "product": sourceLabel = "产品"; break;
            case "arena": sourceLabel = "评测"; break;
            default: sourceLabel = sourceType;
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
}
