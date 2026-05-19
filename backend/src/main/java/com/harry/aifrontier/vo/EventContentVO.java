package com.harry.aifrontier.vo;

import lombok.Data;

@Data
public class EventContentVO {

    private Long contentId;

    private Long linkId;

    private String title;

    private String contentType;

    private String summary;

    private String coverImage;

    private String sourceUrl;

    private String authorName;

    private String publishStatus;
}
