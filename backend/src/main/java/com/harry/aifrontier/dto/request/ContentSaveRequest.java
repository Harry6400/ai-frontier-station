package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentSaveRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String slug;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    private String summary;

    private String coverImage;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long sourceId;

    private String sourceUrl;

    private String authorName;

    @NotBlank(message = "发布状态不能为空")
    private String publishStatus;

    @NotNull(message = "精选级别不能为空")
    private Integer featuredLevel;

    private Integer readingTime;

    private LocalDateTime publishedAt;

    @NotBlank(message = "正文不能为空")
    private String bodyMarkdown;

    private String extraJson;

    private List<Long> tagIds;
}
