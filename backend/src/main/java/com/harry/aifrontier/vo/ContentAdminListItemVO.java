package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentAdminListItemVO {

    private Long id;

    private String title;

    private String slug;

    private String contentType;

    private String summary;

    private String publishStatus;

    private Integer featuredLevel;

    private Integer viewCount;

    private String categoryName;

    private String sourceName;

    private String sourceType;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;

    private List<TagVO> tags;
}
