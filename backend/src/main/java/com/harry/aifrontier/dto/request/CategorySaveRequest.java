package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategorySaveRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String slug;

    private String description;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    @NotNull(message = "启用状态不能为空")
    private Integer isEnabled;
}
