package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ArxivPaperImportRequest {

    @NotBlank(message = "arXiv ID 不能为空")
    private String arxivId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private List<String> authors;

    @NotBlank(message = "摘要不能为空")
    private String abstractText;

    private String pdfUrl;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long sourceId;

    private List<Long> tagIds;

    @NotBlank(message = "发布状态不能为空")
    private String publishStatus;

    private String summary;

    private String bodyMarkdown;
}
