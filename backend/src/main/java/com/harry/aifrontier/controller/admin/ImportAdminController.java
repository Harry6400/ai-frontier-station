package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.dto.request.AiSourceImportRequest;
import com.harry.aifrontier.dto.request.ArxivPaperImportRequest;
import com.harry.aifrontier.dto.request.GitHubRepoImportRequest;
import com.harry.aifrontier.dto.request.HuggingFacePaperImportRequest;
import com.harry.aifrontier.service.ContentService;
import com.harry.aifrontier.vo.ContentDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
public class ImportAdminController {

    private final ContentService contentService;

    @PostMapping("/github-repo")
    public ApiResponse<ContentDetailVO> importGitHubRepo(@Valid @RequestBody GitHubRepoImportRequest request) {
        return ApiResponse.success("GitHub 项目导入成功", contentService.importGitHubRepo(request));
    }

    @PostMapping("/ai-source")
    public ApiResponse<ContentDetailVO> importAiSource(@Valid @RequestBody AiSourceImportRequest request) {
        return ApiResponse.success("AI 来源整理内容创建成功", contentService.importAiSource(request));
    }

    @PostMapping("/arxiv-paper")
    public ApiResponse<ContentDetailVO> importArxivPaper(@Valid @RequestBody ArxivPaperImportRequest request) {
        return ApiResponse.success("arXiv 论文导入成功", contentService.importArxivPaper(request));
    }

    @PostMapping("/huggingface-paper")
    public ApiResponse<ContentDetailVO> importHuggingFacePaper(@Valid @RequestBody HuggingFacePaperImportRequest request) {
        return ApiResponse.success("HuggingFace 论文导入成功", contentService.importHuggingFacePaper(request));
    }
}
