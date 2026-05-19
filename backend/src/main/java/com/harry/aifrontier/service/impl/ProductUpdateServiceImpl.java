package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.service.ProductUpdateService;
import com.harry.aifrontier.util.CandidateValidator;
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
public class ProductUpdateServiceImpl implements ProductUpdateService {

    private final ContentCandidateMapper contentCandidateMapper;
    private final ObjectMapper objectMapper;

    private static final List<RssSource> RSS_SOURCES = List.of(
            new RssSource("OpenAI", "https://openai.com", "/blog/rss.xml",
                    "/index/rss.xml"),
            new RssSource("Anthropic", "https://www.anthropic.com", "/rss.xml", null),
            new RssSource("Google AI", "https://blog.google", "/technology/ai/rss/", null)
    );

    @Override
    public int fetchProductUpdates() {
        log.info("开始获取 AI 产品动态...");
        int totalCount = 0;

        for (RssSource source : RSS_SOURCES) {
            int count = fetchSingleSource(source);
            totalCount += count;
            // 源之间间隔 2 秒
            sleepQuietly(2000);
        }

        log.info("AI 产品动态获取完成，共新增 {} 条", totalCount);
        return totalCount;
    }

    private int fetchSingleSource(RssSource source) {
        log.info("获取 {} RSS: {}{}", source.name, source.baseUrl, source.primaryPath);
        try {
            String xml = fetchRssContent(source);
            if (xml == null) {
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

                    if (existsByUrl(link)) {
                        log.debug("产品动态已存在，跳过: {}", link);
                        continue;
                    }

                    // 识别产品和公司
                    ProductInfo productInfo = identifyProduct(title, link);

                    ContentCandidate candidate = new ContentCandidate();
                    candidate.setSourceType("product");
                    candidate.setExternalId(link);
                    candidate.setTitle(truncate(title, 500));
                    candidate.setUrl(link);
                    candidate.setRawContent(truncate(description, 2000));
                    candidate.setAuthor(source.name);
                    candidate.setPublishedAt(parseRssDate(pubDate));
                    candidate.setStatus("pending");
                    candidate.setMetadataJson(buildMetadataJson(productInfo, source.name));

                    if (CandidateValidator.validate(candidate)) {
                        contentCandidateMapper.insert(candidate);
                        count++;
                        log.info("新增 {} 产品动态: {}", source.name, title);
                    } else {
                        log.debug("数据质量验证未通过，跳过: {}", title);
                    }
                } catch (Exception e) {
                    log.warn("处理 {} 文章出错: {}", source.name, e.getMessage());
                }
            }

            log.info("{} RSS 获取完成，新增 {} 条", source.name, count);
            return count;
        } catch (RestClientException e) {
            log.error("{} RSS 请求失败: {}", source.name, e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("{} RSS 解析失败: {}", source.name, e.getMessage());
            return 0;
        }
    }

    private String fetchRssContent(RssSource source) {
        RestClient client = RestClient.builder().baseUrl(source.baseUrl).build();
        try {
            return client.get().uri(source.primaryPath).retrieve().body(String.class);
        } catch (Exception e) {
            if (source.fallbackPath != null) {
                log.info("主路径失败，尝试备用路径: {}", source.fallbackPath);
                try {
                    return client.get().uri(source.fallbackPath).retrieve().body(String.class);
                } catch (Exception e2) {
                    log.warn("备用路径也失败: {}", e2.getMessage());
                    return null;
                }
            }
            throw e;
        }
    }

    private ProductInfo identifyProduct(String title, String url) {
        String combined = (title + " " + url).toLowerCase();

        if (combined.contains("openai") || combined.contains("chatgpt") || combined.contains("gpt")) {
            return new ProductInfo("ChatGPT", "OpenAI", "update");
        }
        if (combined.contains("anthropic") || combined.contains("claude")) {
            return new ProductInfo("Claude", "Anthropic", "update");
        }
        if (combined.contains("google") || combined.contains("gemini") || combined.contains("bard")) {
            return new ProductInfo("Gemini", "Google", "update");
        }
        if (combined.contains("deepseek")) {
            return new ProductInfo("DeepSeek", "DeepSeek", "update");
        }
        if (combined.contains("qwen") || combined.contains("tongyi") || combined.contains("通义")) {
            return new ProductInfo("Qwen", "阿里云", "update");
        }
        if (combined.contains("meta") || combined.contains("llama")) {
            return new ProductInfo("Llama", "Meta", "update");
        }

        return new ProductInfo("Unknown", "Unknown", "update");
    }

    private List<Map<String, String>> parseRssXml(String xml) throws Exception {
        List<Map<String, String>> items = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
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

    private boolean existsByUrl(String url) {
        return contentCandidateMapper.selectCount(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getUrl, url)
                        .eq(ContentCandidate::getSourceType, "product")
        ) > 0;
    }

    private String buildMetadataJson(ProductInfo info, String sourceName) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("product", info.product);
            metadata.put("company", info.company);
            metadata.put("updateType", info.updateType);
            metadata.put("source", sourceName);
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
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            return OffsetDateTime.parse(dateStr, formatter).toLocalDateTime();
        } catch (Exception e) {
            try {
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

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record RssSource(String name, String baseUrl, String primaryPath, String fallbackPath) {}

    private record ProductInfo(String product, String company, String updateType) {}
}
