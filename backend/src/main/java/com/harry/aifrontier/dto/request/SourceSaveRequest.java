package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SourceSaveRequest {

    @NotBlank(message = "来源名称不能为空")
    private String name;

    private String slug;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    private String websiteUrl;

    private String description;

    @NotNull(message = "启用状态不能为空")
    private Integer isEnabled;
}
