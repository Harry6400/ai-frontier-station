package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.service.GitHubSyncService;
import com.harry.aifrontier.vo.GitHubSyncResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/github-sync")
@RequiredArgsConstructor
public class GitHubSyncController {

    private final GitHubSyncService gitHubSyncService;

    @PostMapping("/repo/{id}")
    public ApiResponse<GitHubSyncResultVO> syncRepo(@PathVariable Long id) {
        return ApiResponse.success("GitHub 仓库同步成功", gitHubSyncService.syncRepo(id));
    }

    @PostMapping("/all")
    public ApiResponse<List<GitHubSyncResultVO>> syncAll() {
        return ApiResponse.success("GitHub 仓库批量同步完成", gitHubSyncService.syncAllGitHubRepos());
    }
}
