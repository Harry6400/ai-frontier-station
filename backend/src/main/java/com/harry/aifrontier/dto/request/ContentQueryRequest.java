package com.harry.aifrontier.dto.request;

import lombok.Data;

@Data
public class ContentQueryRequest {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String contentType;

    private Long categoryId;

    private Long sourceId;

    private String publishStatus;
}
