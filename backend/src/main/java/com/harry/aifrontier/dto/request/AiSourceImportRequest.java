package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AiSourceImportRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    @NotBlank(message = "摘要不能为空")
    private String summary;

    private String coverImage;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long sourceId;

    @NotBlank(message = "来源链接不能为空")
    private String sourceUrl;

    private String sourceTitle;

    private String sourceName;

    private String sourceType;

    private String originalSummary;

    private String originalExcerpt;

    private String sourceBrief;

    private String aiSummary;

    private String recommendationReason;

    private Integer importanceScore;

    private String bodyMarkdown;

    private List<String> tagSuggestions;

    private List<Long> tagIds;
}
