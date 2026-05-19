package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentAdminListItemVO {

    private Long id;

    private String title;

    private String slug;

    private String contentType;

    private String subCategory;

    private String summary;

    private String publishStatus;

    private Integer featuredLevel;

    private Integer viewCount;

    private String categoryName;

    private String sourceName;

    private String sourceType;

    private java.math.BigDecimal trendScore;
    private Integer starGrowth7d;
    private Integer forkCount;
    private String aiTags;
    private String aiDirection;
    private String aiDifficulty;
    private String aiAudience;
    private String aiLearningPath;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}
