package com.harry.aifrontier.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiSourceSummaryVO {

    private String suggestedTitle;

    private String summary;

    private String sourceBrief;

    private String aiSummary;

    private String recommendationReason;

    private Integer importanceScore;

    private List<String> tagSuggestions;

    private Integer readingTime;

    private String bodyMarkdown;

    private String extraJson;
}
