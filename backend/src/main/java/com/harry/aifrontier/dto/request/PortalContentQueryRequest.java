package com.harry.aifrontier.dto.request;

import lombok.Data;

@Data
public class PortalContentQueryRequest {

    private Long pageNum = 1L;

    private Long pageSize = 9L;

    private String keyword;

    private String contentType;

    private Long categoryId;

    private Long tagId;

    private Long sourceId;

    private String sortBy = "latest";
}
