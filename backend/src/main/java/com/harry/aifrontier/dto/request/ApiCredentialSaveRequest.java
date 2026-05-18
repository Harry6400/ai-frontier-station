package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiCredentialSaveRequest {

    @NotBlank(message = "密钥不能为空")
    private String apiKey;

    private String remark;
}
