package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GitHubRepoCandidateVO {

    private String repoFullName;

    private String repoUrl;

    private String description;

    private Integer stars;

    private Integer forks;

    private Integer watchers;

    private String language;

    private List<String> topics;

    private String homepage;

    private String defaultBranch;

    private LocalDateTime updatedAt;

    private String readmeSummary;
}
