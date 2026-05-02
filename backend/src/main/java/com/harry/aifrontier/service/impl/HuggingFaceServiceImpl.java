package com.harry.aifrontier.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.harry.aifrontier.service.HuggingFaceService;
import com.harry.aifrontier.vo.HuggingFacePaperCandidateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HuggingFaceServiceImpl implements HuggingFaceService {

    private static final String HF_API_BASE = "https://huggingface.co";

    @Override
    public List<HuggingFacePaperCandidateVO> getDailyPapers() {
        try {
            RestClient client = RestClient.builder().baseUrl(HF_API_BASE).build();
            JsonNode response = client.get()
                    .uri("/api/daily_papers")
                    .retrieve()
                    .body(JsonNode.class);

            return parsePapers(response);
        } catch (Exception e) {
            log.error("HuggingFace 每日论文获取失败: {}", e.getMessage(), e);
            throw new IllegalArgumentException("HuggingFace 论文获取失败: " + e.getMessage());
        }
    }

    @Override
    public HuggingFacePaperCandidateVO findPaper(String paperId) {
        try {
            RestClient client = RestClient.builder().baseUrl(HF_API_BASE).build();
            JsonNode response = client.get()
                    .uri("/api/papers/{paperId}", paperId)
                    .retrieve()
                    .body(JsonNode.class);

            return parsePaper(response);
        } catch (Exception e) {
            log.error("HuggingFace 论文查询失败: {}", e.getMessage(), e);
            throw new IllegalArgumentException("HuggingFace 论文查询失败: " + e.getMessage());
        }
    }

    private List<HuggingFacePaperCandidateVO> parsePapers(JsonNode response) {
        List<HuggingFacePaperCandidateVO> papers = new ArrayList<>();
        if (response == null || !response.isArray()) {
            return papers;
        }

        for (JsonNode item : response) {
            try {
                HuggingFacePaperCandidateVO paper = parsePaperItem(item);
                if (paper != null) {
                    papers.add(paper);
                }
            } catch (Exception e) {
                log.warn("解析 HuggingFace 论文条目失败: {}", e.getMessage());
            }
        }
        return papers;
    }

    private HuggingFacePaperCandidateVO parsePaperItem(JsonNode item) {
        HuggingFacePaperCandidateVO paper = new HuggingFacePaperCandidateVO();

        JsonNode paperNode = item.has("paper") ? item.get("paper") : item;

        paper.setPaperId(getStringValue(paperNode, "id"));
        paper.setTitle(getStringValue(paperNode, "title"));
        paper.setAbstractText(getStringValue(paperNode, "summary"));
        paper.setLikes(getIntValue(paperNode, "likes"));
        paper.setComments(getIntValue(paperNode, "comments"));

        String url = getStringValue(paperNode, "url");
        if (url != null && !url.isBlank()) {
            paper.setHtmlUrl(url);
        } else if (paper.getPaperId() != null) {
            paper.setHtmlUrl("https://huggingface.co/papers/" + paper.getPaperId());
        }

        paper.setPublishedAt(parseDateTime(getStringValue(paperNode, "publishedAt")));

        List<String> authors = new ArrayList<>();
        if (paperNode.has("authors") && paperNode.get("authors").isArray()) {
            for (JsonNode author : paperNode.get("authors")) {
                String name = getStringValue(author, "name");
                if (name != null && !name.isBlank()) {
                    authors.add(name);
                }
            }
        }
        paper.setAuthors(authors);

        return paper;
    }

    private HuggingFacePaperCandidateVO parsePaper(JsonNode paperNode) {
        HuggingFacePaperCandidateVO paper = new HuggingFacePaperCandidateVO();

        paper.setPaperId(getStringValue(paperNode, "id"));
        paper.setTitle(getStringValue(paperNode, "title"));
        paper.setAbstractText(getStringValue(paperNode, "summary"));
        paper.setLikes(getIntValue(paperNode, "likes"));
        paper.setComments(getIntValue(paperNode, "comments"));

        String url = getStringValue(paperNode, "url");
        if (url != null && !url.isBlank()) {
            paper.setHtmlUrl(url);
        } else if (paper.getPaperId() != null) {
            paper.setHtmlUrl("https://huggingface.co/papers/" + paper.getPaperId());
        }

        paper.setPublishedAt(parseDateTime(getStringValue(paperNode, "publishedAt")));

        List<String> authors = new ArrayList<>();
        if (paperNode.has("authors") && paperNode.get("authors").isArray()) {
            for (JsonNode author : paperNode.get("authors")) {
                String name = getStringValue(author, "name");
                if (name != null && !name.isBlank()) {
                    authors.add(name);
                }
            }
        }
        paper.setAuthors(authors);

        return paper;
    }

    private String getStringValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value.trim();
    }

    private Integer getIntValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asInt();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return null;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            return odt.toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
