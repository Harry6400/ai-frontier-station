package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentExternalRefVO {

    private Long id;

    private String refType;

    private String externalId;

    private String externalUrl;

    private String rawPayloadJson;

    private LocalDateTime syncedAt;
}
