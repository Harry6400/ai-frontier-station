package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContentStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String publishStatus;
}
