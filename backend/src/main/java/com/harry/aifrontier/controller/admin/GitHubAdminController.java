package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.service.GitHubService;
import com.harry.aifrontier.vo.GitHubRepoCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/github")
@RequiredArgsConstructor
public class GitHubAdminController {

    private final GitHubService gitHubService;

    @GetMapping("/repo")
    public ApiResponse<GitHubRepoCandidateVO> findRepo(@RequestParam String fullName) {
        return ApiResponse.success(gitHubService.findRepo(fullName));
    }

    @GetMapping("/search")
    public ApiResponse<PageResult<GitHubRepoCandidateVO>> searchRepos(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "stars") String sort,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return ApiResponse.success(gitHubService.searchRepos(keyword, sort, pageNum, pageSize));
    }
}
