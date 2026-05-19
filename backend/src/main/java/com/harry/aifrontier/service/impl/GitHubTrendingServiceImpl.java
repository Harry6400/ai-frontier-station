package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.GitHubTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubTrendingServiceImpl implements GitHubTrendingService {

    private final ContentCandidateMapper contentCandidateMapper;
    private final ApiCredentialService apiCredentialService;
    private final ObjectMapper objectMapper;

    @Value("${app.github.token:}")
    private String githubToken;

    @Value("${app.github.base-url:https://api.github.com}")
    private String githubBaseUrl;

    private static final Set<String> AI_KEYWORDS = Set.of(
            "ai", "llm", "gpt", "machine-learning", "deep-learning",
            "neural", "transformer", "diffusion", "agent", "rag",
            "embedding", "fine-tune", "inference", "model", "openai",
            "anthropic", "gemini", "claude"
    );

    @Override
    public int fetchTrending(String language, String since) {
        // 计算7天前的日期
        String sevenDaysAgo = Instant.now().minus(Duration.ofDays(7))
                .atOffset(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        log.info("开始获取 GitHub AI Trending 仓库 (过去7天活跃, stars>50): since={}", sevenDaysAgo);

        try {
            // 使用 GitHub Search API 搜索最近活跃的 AI 仓库
            String query = "stars:>50 pushed:>" + sevenDaysAgo
                    + " (topic:ai OR topic:machine-learning OR topic:llm OR topic:deep-learning"
                    + " OR topic:generative-ai OR topic:ai-agent OR topic:diffusion"
                    + " OR topic:nlp OR topic:computer-vision)";

            JsonNode result = githubClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/repositories")
                            .queryParam("q", query)
                            .queryParam("sort", "stars")
                            .queryParam("order", "desc")
                            .queryParam("per_page", 30)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (result == null || !result.has("items")) {
                log.warn("GitHub Trending 响应格式异常");
                return 0;
            }

            int count = 0;
            int updated = 0;
            for (JsonNode item : result.path("items")) {
                try {
                    String fullName = text(item, "full_name");
                    if (fullName.isBlank()) {
                        continue;
                    }

                    // 计算趋势分数
                    BigDecimal trendScore = calculateTrendScore(item);
                    int starGrowth7d = estimateStarGrowth7d(item);
                    int forks = item.path("forks_count").asInt(0);

                    // 构建候选内容
                    ContentCandidate candidate = buildCandidate(item, trendScore, starGrowth7d, forks);

                    // 检查是否已存在
                    ContentCandidate existing = findByExternalId(fullName);
                    if (existing != null) {
                        // 更新已存在的候选内容
                        updateExistingCandidate(existing, item, trendScore, starGrowth7d, forks);
                        updated++;
                        log.debug("更新已存在的仓库: {} (score={})", fullName, trendScore);
                        continue;
                    }

                    contentCandidateMapper.insert(candidate);
                    count++;
                    log.info("新增 GitHub AI 仓库: {} (score={}, stars={})",
                            fullName, trendScore, item.path("stargazers_count").asInt(0));
                } catch (Exception e) {
                    log.warn("处理 GitHub 仓库时出错: {}", e.getMessage());
                }
            }

            log.info("GitHub AI Trending 获取完成: 新增 {} 个, 更新 {} 个仓库", count, updated);
            return count;

        } catch (RestClientException e) {
            log.error("GitHub Trending API 请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("GitHub Trending 获取失败: " + e.getMessage(), e);
        }
    }

    // ==================== Trend Score 计算 ====================

    private BigDecimal calculateTrendScore(JsonNode repo) {
        int stars = repo.path("stargazers_count").asInt(0);
        int forks = repo.path("forks_count").asInt(0);
        String pushedAt = repo.path("pushed_at").asText("");
        String createdAt = repo.path("created_at").asText("");

        // 星标增长率: 新仓库高星标 = 高增长
        long repoAgeDays = daysSince(createdAt);
        double starGrowthScore = repoAgeDays > 0
                ? Math.min(stars / (double) repoAgeDays * 7, 50000)
                : stars;

        // 活跃度: 最近推送时间
        long daysSincePush = daysSince(pushedAt);
        double recencyScore = Math.max(0, 100 - daysSincePush * 10);

        // 社区参与度: forks/stars 比率
        double forkRatio = stars > 0 ? (double) forks / stars : 0;
        double forkScore = Math.min(forkRatio * 500, 100);

        // AI 相关性: 检查描述和主题
        double aiScore = calculateAiRelevance(repo);

        // 时间衰减: 惩罚过于老旧的仓库
        double timeDecay = Math.max(0, 1.0 - repoAgeDays / 365.0 * 0.5);

        // 最终分数 = (增长率*0.35 + 活跃度*0.15 + 社区参与*0.10 + AI相关*0.10) * 时间衰减
        double score = (starGrowthScore * 0.35
                + recencyScore * 0.15
                + forkScore * 0.10
                + aiScore * 0.10) * timeDecay;

        return BigDecimal.valueOf(Math.round(score * 100) / 100.0);
    }

    private double calculateAiRelevance(JsonNode repo) {
        String desc = repo.path("description").asText("").toLowerCase();
        JsonNode topics = repo.path("topics");

        int matches = 0;
        for (String kw : AI_KEYWORDS) {
            if (desc.contains(kw)) matches++;
        }
        if (topics != null) {
            for (JsonNode t : topics) {
                if (AI_KEYWORDS.contains(t.asText().toLowerCase())) matches++;
            }
        }
        return Math.min(matches * 20, 100);
    }

    private int estimateStarGrowth7d(JsonNode repo) {
        int stars = repo.path("stargazers_count").asInt(0);
        String createdAt = repo.path("created_at").asText("");
        long repoAgeDays = daysSince(createdAt);
        if (repoAgeDays <= 0) return stars;
        // 估算每周星标增长数
        double weeklyRate = (double) stars / repoAgeDays * 7;
        return (int) Math.min(weeklyRate, 10000);
    }

    private long daysSince(String isoDate) {
        try {
            return Duration.between(
                    Instant.parse(isoDate),
                    Instant.now()
            ).toDays();
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 候选内容构建 ====================

    private ContentCandidate buildCandidate(JsonNode item, BigDecimal trendScore, int starGrowth7d, int forks) {
        String fullName = text(item, "full_name");
        if (fullName.isBlank()) {
            return null;
        }

        ContentCandidate candidate = new ContentCandidate();
        candidate.setSourceType("github");
        candidate.setExternalId(fullName);

        // 标题: full_name + description
        String description = text(item, "description");
        String title = description.isBlank() ? fullName : fullName + ": " + description;
        candidate.setTitle(truncate(title, 500));

        candidate.setUrl(text(item, "html_url"));
        candidate.setRawContent(description);
        candidate.setAuthor(item.path("owner").path("login").asText(""));
        candidate.setPublishedAt(parseDateTime(text(item, "created_at")));
        candidate.setStatus("pending");

        // 设置趋势雷达字段
        candidate.setTrendScore(trendScore);
        candidate.setStarGrowth7d(starGrowth7d);
        candidate.setForkCount(forks);

        // 构建 metadataJson
        candidate.setMetadataJson(buildMetadataJson(item));

        return candidate;
    }

    private String buildMetadataJson(JsonNode item) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("stars", item.path("stargazers_count").asInt(0));
            metadata.put("forks", item.path("forks_count").asInt(0));
            metadata.put("language", text(item, "language"));

            List<String> topics = new ArrayList<>();
            item.path("topics").forEach(t -> {
                String topic = t.asText("");
                if (!topic.isBlank()) {
                    topics.add(topic);
                }
            });
            metadata.put("topics", topics);

            String pushedAt = text(item, "pushed_at");
            String createdAt = text(item, "created_at");
            metadata.put("pushedAt", pushedAt);
            metadata.put("createdAt", createdAt);
            metadata.put("repoAgeDays", daysSince(createdAt));

            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("序列化 metadata 失败: {}", e.getMessage());
            return "{}";
        }
    }

    // ==================== 更新已存在的候选内容 ====================

    private void updateExistingCandidate(ContentCandidate existing, JsonNode item,
                                         BigDecimal trendScore, int starGrowth7d, int forks) {
        LambdaUpdateWrapper<ContentCandidate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ContentCandidate::getId, existing.getId())
                .set(ContentCandidate::getTrendScore, trendScore)
                .set(ContentCandidate::getStarGrowth7d, starGrowth7d)
                .set(ContentCandidate::getForkCount, forks)
                .set(ContentCandidate::getMetadataJson, buildMetadataJson(item))
                .set(ContentCandidate::getUpdatedAt, LocalDateTime.now());
        contentCandidateMapper.update(null, wrapper);
    }

    // ==================== 查询与工具方法 ====================

    private ContentCandidate findByExternalId(String externalId) {
        LambdaQueryWrapper<ContentCandidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentCandidate::getExternalId, externalId)
               .eq(ContentCandidate::getSourceType, "github");
        return contentCandidateMapper.selectOne(wrapper);
    }

    private RestClient githubClient() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(githubBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "AI-Frontier-Station");
        String token = apiCredentialService.resolveGitHubToken(githubToken);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
        }
        return builder.build();
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? "" : value.trim();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
