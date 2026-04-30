package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiSourceSummaryRequest {

    @NotBlank(message = "来源标题不能为空")
    private String sourceTitle;

    @NotBlank(message = "来源链接不能为空")
    private String sourceUrl;

    private String sourceName;

    private String sourceType;

    private String contentType;

    private String originalSummary;

    private String originalExcerpt;

    private String imageUrl;

    private String instruction;
}
