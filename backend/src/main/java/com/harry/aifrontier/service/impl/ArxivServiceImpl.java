package com.harry.aifrontier.service.impl;

import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.service.ArxivService;
import com.harry.aifrontier.vo.ArxivPaperCandidateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ArxivServiceImpl implements ArxivService {

    private static final String ARXIV_API_BASE = "https://export.arxiv.org/api";
    private static final Pattern ARXIV_ID_PATTERN = Pattern.compile("(\\d+\\.\\d+)");

    @Override
    public PageResult<ArxivPaperCandidateVO> searchPapers(String query, int maxResults, int start) {
        final int effectiveMax = maxResults <= 0 ? 10 : Math.min(maxResults, 50);
        final int effectiveStart = Math.max(start, 0);

        try {
            RestClient client = RestClient.builder().baseUrl(ARXIV_API_BASE).build();
            String xml = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("search_query", query)
                            .queryParam("start", effectiveStart)
                            .queryParam("max_results", effectiveMax)
                            .queryParam("sortBy", "submittedDate")
                            .queryParam("sortOrder", "descending")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseSearchResponse(xml);
        } catch (Exception e) {
            log.error("arXiv 搜索失败: {}", e.getMessage(), e);
            throw new IllegalArgumentException("arXiv 搜索失败: " + e.getMessage());
        }
    }

    @Override
    public ArxivPaperCandidateVO findPaper(String arxivId) {
        try {
            String cleanId = extractArxivId(arxivId);
            RestClient client = RestClient.builder().baseUrl(ARXIV_API_BASE).build();
            String xml = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("id_list", cleanId)
                            .queryParam("max_results", 1)
                            .build())
                    .retrieve()
                    .body(String.class);

            PageResult<ArxivPaperCandidateVO> result = parseSearchResponse(xml);
            if (result.getRecords().isEmpty()) {
                throw new IllegalArgumentException("未找到 arXiv 论文: " + arxivId);
            }
            return result.getRecords().get(0);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("arXiv 论文查询失败: {}", e.getMessage(), e);
            throw new IllegalArgumentException("arXiv 论文查询失败: " + e.getMessage());
        }
    }

    private PageResult<ArxivPaperCandidateVO> parseSearchResponse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            Element root = doc.getDocumentElement();
            long totalResults = parseLong(getElementText(root, "totalResults", "http://a9.com/-/spec/opensearch/1.1/"), 0);
            int startIndex = parseInt(getElementText(root, "startIndex", "http://a9.com/-/spec/opensearch/1.1/"), 0);
            int itemsPerPage = parseInt(getElementText(root, "itemsPerPage", "http://a9.com/-/spec/opensearch/1.1/"), 10);

            List<ArxivPaperCandidateVO> papers = new ArrayList<>();
            NodeList entries = root.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                ArxivPaperCandidateVO paper = parseEntry(entry);
                if (paper != null) {
                    papers.add(paper);
                }
            }

            int page = startIndex / itemsPerPage + 1;
            return PageResult.of(totalResults, (long) page, (long) itemsPerPage, papers);
        } catch (Exception e) {
            log.error("解析 arXiv XML 失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析 arXiv 响应失败", e);
        }
    }

    private ArxivPaperCandidateVO parseEntry(Element entry) {
        try {
            ArxivPaperCandidateVO paper = new ArxivPaperCandidateVO();

            String idUrl = getElementText(entry, "id", null);
            paper.setArxivId(extractArxivId(idUrl));
            paper.setTitle(cleanText(getElementText(entry, "title", null)));
            paper.setAbstractText(cleanText(getElementText(entry, "summary", null)));
            paper.setPublishedAt(parseDateTime(getElementText(entry, "published", null)));
            paper.setUpdatedAt(parseDateTime(getElementText(entry, "updated", null)));

            List<String> authors = new ArrayList<>();
            NodeList authorNodes = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "author");
            for (int i = 0; i < authorNodes.getLength(); i++) {
                Element authorEl = (Element) authorNodes.item(i);
                String name = getElementText(authorEl, "name", null);
                if (name != null && !name.isBlank()) {
                    authors.add(name);
                }
            }
            paper.setAuthors(authors);

            List<String> categories = new ArrayList<>();
            String primaryCategory = null;
            NodeList categoryNodes = entry.getElementsByTagNameNS("http://arxiv.org/schemas/atom", "primary_category");
            if (categoryNodes.getLength() > 0) {
                primaryCategory = ((Element) categoryNodes.item(0)).getAttribute("term");
            }
            NodeList catNodes = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "category");
            for (int i = 0; i < catNodes.getLength(); i++) {
                String term = ((Element) catNodes.item(i)).getAttribute("term");
                if (term != null && !term.isBlank()) {
                    categories.add(term);
                }
            }
            paper.setCategories(categories);
            paper.setPrimaryCategory(primaryCategory != null ? primaryCategory : (categories.isEmpty() ? null : categories.get(0)));

            NodeList links = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "link");
            for (int i = 0; i < links.getLength(); i++) {
                Element link = (Element) links.item(i);
                String rel = link.getAttribute("rel");
                String type = link.getAttribute("type");
                String href = link.getAttribute("href");
                if ("alternate".equals(rel) && href != null) {
                    paper.setHtmlUrl(href);
                }
                if ("related".equals(rel) && "application/pdf".equals(type) && href != null) {
                    paper.setPdfUrl(href);
                }
            }

            if (paper.getHtmlUrl() == null && paper.getArxivId() != null) {
                paper.setHtmlUrl("https://arxiv.org/abs/" + paper.getArxivId());
            }
            if (paper.getPdfUrl() == null && paper.getArxivId() != null) {
                paper.setPdfUrl("https://arxiv.org/pdf/" + paper.getArxivId());
            }

            return paper;
        } catch (Exception e) {
            log.warn("解析 arXiv 条目失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractArxivId(String input) {
        if (input == null) return "";
        Matcher matcher = ARXIV_ID_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return input.trim();
    }

    private String getElementText(Element parent, String localName, String namespaceUri) {
        NodeList nodes;
        if (namespaceUri != null) {
            nodes = parent.getElementsByTagNameNS(namespaceUri, localName);
        } else {
            nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", localName);
            if (nodes.getLength() == 0) {
                nodes = parent.getElementsByTagName(localName);
            }
        }
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }

    private String cleanText(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s+", " ").trim();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return null;
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            return zdt.toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private long parseLong(String value, long defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
