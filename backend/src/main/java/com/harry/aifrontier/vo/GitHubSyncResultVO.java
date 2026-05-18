package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GitHubSyncResultVO {

    private Long externalRefId;

    private String repoFullName;

    private Integer stars;

    private Integer forks;

    private Integer watchers;

    private String language;

    private String syncStatus;

    private String syncError;

    private LocalDateTime syncedAt;
}
