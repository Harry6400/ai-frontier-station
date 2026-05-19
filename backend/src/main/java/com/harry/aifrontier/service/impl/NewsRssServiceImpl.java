package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.service.NewsRssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsRssServiceImpl implements NewsRssService {

    private final ContentCandidateMapper contentCandidateMapper;
    private final ObjectMapper objectMapper;

    private static final List<String> AI_KEYWORDS = List.of(
            "ai", "llm", "gpt", "model", "machine learning", "deep learning",
            "neural", "transformer", "diffusion", "openai", "anthropic",
            "google ai", "deepseek"
    );

    @Override
    public int fetchNews() {
        log.info("开始获取 AI 新闻...");
        int totalCount = 0;

        totalCount += fetchTechCrunchRss();
        totalCount += fetchHackerNews();

        log.info("AI 新闻获取完成，共新增 {} 条", totalCount);
        return totalCount;
    }

    private int fetchTechCrunchRss() {
        log.info("获取 TechCrunch RSS...");
        try {
            String xml = RestClient.builder()
                    .baseUrl("https://techcrunch.com")
                    .build()
                    .get()
                    .uri("/feed/")
                    .retrieve()
                    .body(String.class);

            if (xml == null || xml.isBlank()) {
                log.warn("TechCrunch RSS 返回空内容");
                return 0;
            }

            List<Map<String, String>> items = parseRssXml(xml);
            int count = 0;

            for (Map<String, String> item : items) {
                try {
                    String title = item.getOrDefault("title", "");
                    String link = item.getOrDefault("link", "");
                    String description = item.getOrDefault("description", "");
                    String pubDate = item.getOrDefault("pubDate", "");

                    if (title.isBlank() || link.isBlank()) {
                        continue;
                    }

                    if (!isAiRelated(title, description)) {
                        continue;
                    }

                    if (existsByUrl(link)) {
                        log.debug("TechCrunch 文章已存在，跳过: {}", link);
                        continue;
                    }

                    ContentCandidate candidate = new ContentCandidate();
                    candidate.setSourceType("news");
                    candidate.setExternalId(link);
                    candidate.setTitle(truncate(title, 500));
                    candidate.setUrl(link);
                    candidate.setRawContent(truncate(description, 2000));
                    candidate.setAuthor("TechCrunch");
                    candidate.setPublishedAt(parseRssDate(pubDate));
                    candidate.setStatus("pending");
                    candidate.setMetadataJson(buildMetadataJson("techcrunch", null, null));

                    contentCandidateMapper.insert(candidate);
                    count++;
                    log.info("新增 TechCrunch 新闻: {}", title);
                } catch (Exception e) {
                    log.warn("处理 TechCrunch 文章出错: {}", e.getMessage());
                }
            }

            log.info("TechCrunch RSS 获取完成，新增 {} 条", count);
            return count;
        } catch (RestClientException e) {
            log.error("TechCrunch RSS 请求失败: {}", e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            log.error("TechCrunch RSS 解析失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    private int fetchHackerNews() {
        log.info("获取 Hacker News 热门...");
        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://hacker-news.firebaseio.com")
                    .build();

            JsonNode topStories = client.get()
                    .uri("/v0/topstories.json")
                    .retrieve()
                    .body(JsonNode.class);

            if (topStories == null || !topStories.isArray()) {
                log.warn("Hacker News top stories 响应格式异常");
                return 0;
            }

            int count = 0;
            int limit = Math.min(topStories.size(), 20);

            for (int i = 0; i < limit; i++) {
                try {
                    int storyId = topStories.get(i).asInt();
                    JsonNode story = client.get()
                            .uri("/v0/item/{id}.json", storyId)
                            .retrieve()
                            .body(JsonNode.class);

                    if (story == null) {
                        continue;
                    }

                    String title = story.path("title").asText("");
                    String url = story.path("url").asText("");
                    String storyText = story.path("text").asText("");
                    int score = story.path("score").asInt(0);
                    String author = story.path("by").asText("");
                    long time = story.path("time").asLong(0);
                    int descendants = story.path("descendants").asInt(0);

                    if (title.isBlank()) {
                        continue;
                    }

                    // 如果没有外部链接，使用 HN 自身链接
                    if (url.isBlank()) {
                        url = "https://news.ycombinator.com/item?id=" + storyId;
                    }

                    if (!isAiRelated(title, "")) {
                        continue;
                    }

                    String hnId = "hn_" + storyId;
                    if (existsByExternalId(hnId)) {
                        log.debug("HN 故事已存在，跳过: {}", hnId);
                        continue;
                    }

                    ContentCandidate candidate = new ContentCandidate();
                    candidate.setSourceType("news");
                    candidate.setExternalId(hnId);
                    candidate.setTitle(truncate(title, 500));
                    candidate.setUrl(url);
                    // Build raw_content with actual story content
                    String rawContent;
                    if (!storyText.isBlank()) {
                        // Ask HN, Show HN, etc. have actual text content
                        rawContent = storyText;
                    } else if (!url.isBlank()) {
                        // Regular link posts: provide title + URL so AI knows what the story is about
                        rawContent = "Title: " + title + "\nURL: " + url + "\n来源: Hacker News";
                    } else {
                        rawContent = title;
                    }

                    candidate.setRawContent(truncate(rawContent, 2000));
                    candidate.setAuthor(author);
                    candidate.setPublishedAt(time > 0
                            ? LocalDateTime.ofEpochSecond(time, 0, java.time.ZoneOffset.UTC)
                            : null);
                    candidate.setStatus("pending");
                    candidate.setMetadataJson(buildMetadataJson("hackernews", score, descendants));

                    contentCandidateMapper.insert(candidate);
                    count++;
                    log.info("新增 HN 新闻: {} (score={})", title, score);
                } catch (Exception e) {
                    log.warn("处理 HN 故事出错: {}", e.getMessage());
                }
            }

            log.info("Hacker News 获取完成，新增 {} 条", count);
            return count;
        } catch (RestClientException e) {
            log.error("Hacker News API 请求失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    private List<Map<String, String>> parseRssXml(String xml) throws Exception {
        List<Map<String, String>> items = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        // 防止 XXE 攻击
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        NodeList itemNodes = doc.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element item = (Element) itemNodes.item(i);
            Map<String, String> map = new HashMap<>();
            map.put("title", getTagText(item, "title"));
            map.put("link", getTagText(item, "link"));
            map.put("description", getTagText(item, "description"));
            map.put("pubDate", getTagText(item, "pubDate"));
            items.add(map);
        }
        return items;
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text != null ? text.trim() : "";
    }

    private boolean isAiRelated(String title, String description) {
        String combined = (title + " " + description).toLowerCase();
        for (String keyword : AI_KEYWORDS) {
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
                        .eq(ContentCandidate::getSourceType, "news")
        ) > 0;
    }

    private boolean existsByExternalId(String externalId) {
        return contentCandidateMapper.selectCount(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getExternalId, externalId)
                        .eq(ContentCandidate::getSourceType, "news")
        ) > 0;
    }

    private String buildMetadataJson(String platform, Integer score, Integer comments) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("platform", platform);
            if (score != null) {
                metadata.put("score", score);
            }
            if (comments != null) {
                metadata.put("comments", comments);
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("序列化 metadata 失败: {}", e.getMessage());
            return "{}";
        }
    }

    private LocalDateTime parseRssDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            // RSS 标准日期格式: "Wed, 02 Oct 2024 07:00:00 +0000"
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            return OffsetDateTime.parse(dateStr, formatter).toLocalDateTime();
        } catch (Exception e) {
            try {
                // 备用格式
                return OffsetDateTime.parse(dateStr).toLocalDateTime();
            } catch (Exception e2) {
                log.debug("无法解析日期: {}", dateStr);
                return null;
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
