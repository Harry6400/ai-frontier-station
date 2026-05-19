package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.service.ToolsRssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolsRssServiceImpl implements ToolsRssService {

    private final ContentCandidateMapper contentCandidateMapper;
    private final ObjectMapper objectMapper;

    private static final List<String> TOOL_KEYWORDS = List.of(
            "tool", "workflow", "setup", "deploy", "benchmark", "compare",
            "guide", "tutorial", "tips", "trick", "hack", "prompt",
            "agent", "cursor", "copilot", "vscode", "ollama", "vllm", "comfyui"
    );

    private static final List<String> SUBREDDITS = List.of(
            "MachineLearning", "LocalLLaMA"
    );

    @Override
    public int fetchTools() {
        log.info("开始获取 AI 工具与实践...");
        int totalCount = 0;

        for (String subreddit : SUBREDDITS) {
            int count = fetchSubreddit(subreddit);
            totalCount += count;
            // 子版块之间间隔 2 秒
            sleepQuietly(2000);
        }

        log.info("AI 工具与实践获取完成，共新增 {} 条", totalCount);
        return totalCount;
    }

    private int fetchSubreddit(String subreddit) {
        log.info("获取 Reddit r/{} 热门帖子...", subreddit);
        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://www.reddit.com")
                    .defaultHeader(HttpHeaders.USER_AGENT, "AI-Frontier-Station/1.0 (Educational Project)")
                    .build();

            JsonNode response = client.get()
                    .uri("/r/{subreddit}/hot.json?limit=20", subreddit)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("data") || !response.path("data").has("children")) {
                log.warn("Reddit r/{} 响应格式异常", subreddit);
                return 0;
            }

            int count = 0;
            JsonNode children = response.path("data").path("children");

            for (JsonNode child : children) {
                try {
                    JsonNode post = child.path("data");

                    String title = post.path("title").asText("");
                    String permalink = post.path("permalink").asText("");
                    String selftext = post.path("selftext").asText("");
                    String author = post.path("author").asText("");
                    int upvotes = post.path("ups").asInt(0);
                    int comments = post.path("num_comments").asInt(0);
                    long created = post.path("created_utc").asLong(0);
                    boolean isSelf = post.path("is_self").asBoolean(true);

                    // 构建完整 URL
                    String url = "https://www.reddit.com" + permalink;

                    if (title.isBlank()) {
                        continue;
                    }

                    // 过滤工具/实践相关内容
                    if (!isToolRelated(title, selftext)) {
                        continue;
                    }

                    if (existsByUrl(url)) {
                        log.debug("Reddit 帖子已存在，跳过: {}", url);
                        continue;
                    }

                    String rawContent = selftext.isBlank()
                            ? title
                            : (selftext.length() > 2000 ? selftext.substring(0, 2000) + "..." : selftext);

                    ContentCandidate candidate = new ContentCandidate();
                    candidate.setSourceType("tools");
                    candidate.setExternalId(permalink);
                    candidate.setTitle(truncate(title, 500));
                    candidate.setUrl(url);
                    candidate.setRawContent(rawContent);
                    candidate.setAuthor(author);
                    candidate.setPublishedAt(created > 0
                            ? LocalDateTime.ofEpochSecond(created, 0, java.time.ZoneOffset.UTC)
                            : null);
                    candidate.setStatus("pending");
                    candidate.setMetadataJson(buildMetadataJson(subreddit, upvotes, comments));

                    contentCandidateMapper.insert(candidate);
                    count++;
                    log.info("新增 r/{} 帖子: {} (upvotes={})", subreddit, title, upvotes);
                } catch (Exception e) {
                    log.warn("处理 Reddit 帖子出错: {}", e.getMessage());
                }
            }

            log.info("Reddit r/{} 获取完成，新增 {} 条", subreddit, count);
            return count;
        } catch (RestClientException e) {
            log.error("Reddit r/{} 请求失败: {}", subreddit, e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("Reddit r/{} 解析失败: {}", subreddit, e.getMessage(), e);
            return 0;
        }
    }

    private boolean isToolRelated(String title, String selftext) {
        String combined = (title + " " + selftext).toLowerCase();
        for (String keyword : TOOL_KEYWORDS) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean existsByUrl(String url) {
        return contentCandidateMapper.selectCount(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getUrl, url)
                        .eq(ContentCandidate::getSourceType, "tools")
        ) > 0;
    }

    private String buildMetadataJson(String subreddit, int upvotes, int comments) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("platform", "reddit");
            metadata.put("subreddit", subreddit);
            metadata.put("upvotes", upvotes);
            metadata.put("comments", comments);
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("序列化 metadata 失败: {}", e.getMessage());
            return "{}";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
