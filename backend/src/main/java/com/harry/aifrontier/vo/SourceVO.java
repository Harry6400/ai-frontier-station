package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SourceVO {

    private Long id;

    private String name;

    private String slug;

    private String sourceType;

    private String websiteUrl;

    private String description;

    private Integer isEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
