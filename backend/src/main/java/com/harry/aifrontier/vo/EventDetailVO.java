package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventDetailVO {

    private Long id;

    private String title;

    private String summary;

    private String description;

    private String eventDate;

    private List<String> tags;

    private Integer contentCount;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<EventContentVO> contents;

    private String status;
}
