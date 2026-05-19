package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PortalContentQueryRequest {

    @Min(value = 1, message = "页码最小为1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Long pageSize = 20L;

    private String keyword;

    private String contentType;

    private Long categoryId;

    private Long sourceId;

    private String sortBy = "latest";

    private String subCategory;
}
