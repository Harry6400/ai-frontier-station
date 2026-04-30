package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiCredentialStatusVO {

    private String provider;

    private Boolean enabled;

    private String maskedKey;

    private String source;

    private String remark;

    private LocalDateTime updatedAt;
}
