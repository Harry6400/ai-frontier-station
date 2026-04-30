package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_content")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String slug;

    private String contentType;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private Long sourceId;

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
}
