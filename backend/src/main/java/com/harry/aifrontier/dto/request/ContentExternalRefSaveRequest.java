package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentExternalRefSaveRequest {

    @NotBlank(message = "外部引用类型不能为空")
    private String refType;

    private String externalId;

    private String externalUrl;

    private String rawPayloadJson;

    private LocalDateTime syncedAt;
}
