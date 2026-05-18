package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_content_external_ref")
public class ContentExternalRef {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;

    private String refType;

    private String externalId;

    private String externalUrl;

    private String rawPayloadJson;

    private LocalDateTime syncedAt;

    private Integer stars;

    private Integer forks;

    private Integer watchers;

    private String language;

    private LocalDateTime lastSyncedAt;

    private String syncStatus;

    private String syncError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
