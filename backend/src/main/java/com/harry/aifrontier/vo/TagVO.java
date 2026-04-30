package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagVO {

    private Long id;

    private String name;

    private String slug;

    private String color;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
