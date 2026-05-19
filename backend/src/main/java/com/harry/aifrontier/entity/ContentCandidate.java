package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_content_candidate")
public class ContentCandidate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceType;      // github/news/arena/tools/product
    private String externalId;      // owner/repo for github
    private String title;
    private String url;
    private String rawContent;      // original content
    private String author;
    private LocalDateTime publishedAt;
    private String metadataJson;    // structured data (stars, forks, etc.)
    private String aiSummary;       // AI generated summary
    private String aiBody;          // AI generated body
    private String status;          // pending/approved/rejected/imported
    private Long contentId;         // linked Content ID after import
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TrendRadar fields
    private java.math.BigDecimal trendScore;
    private Integer starGrowth7d;
    private Integer forkCount;
}
