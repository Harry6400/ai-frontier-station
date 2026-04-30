package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentDetailVO {

    private Long id;

    private String title;

    private String slug;

    private String contentType;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private String categoryName;

    private Long sourceId;

    private String sourceName;

    private String sourceType;

    private String sourceUrl;

    private String authorName;

    private String publishStatus;

    private Integer featuredLevel;

    private Integer viewCount;

    private Integer readingTime;

    private LocalDateTime publishedAt;

    private String bodyMarkdown;

    private String extraJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<TagVO> tags;

    private List<ContentExternalRefVO> externalRefs;
}
