package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GitHubRepoImportRequest {

    @NotBlank(message = "仓库全名不能为空")
    private String repoFullName;

    @NotBlank(message = "仓库链接不能为空")
    private String repoUrl;

    @NotBlank(message = "项目简介不能为空")
    private String description;

    @Min(value = 0, message = "Star 数不能小于 0")
    private Integer stars;

    private String language;

    private String topics;

    private String homepage;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long sourceId;

    private List<Long> tagIds;

    @NotBlank(message = "发布状态不能为空")
    private String publishStatus;
}
