package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagSaveRequest {

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String slug;

    private String color;

    private String description;
}
