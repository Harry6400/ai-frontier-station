package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private Integer sortOrder;

    private Integer isEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
