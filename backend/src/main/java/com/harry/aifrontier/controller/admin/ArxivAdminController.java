package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.service.ArxivService;
import com.harry.aifrontier.vo.ArxivPaperCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/arxiv")
@RequiredArgsConstructor
public class ArxivAdminController {

    private final ArxivService arxivService;

    @GetMapping("/search")
    public ApiResponse<PageResult<ArxivPaperCandidateVO>> searchPapers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults,
            @RequestParam(defaultValue = "0") int start) {
        return ApiResponse.success(arxivService.searchPapers(query, maxResults, start));
    }

    @GetMapping("/paper")
    public ApiResponse<ArxivPaperCandidateVO> findPaper(@RequestParam String arxivId) {
        return ApiResponse.success(arxivService.findPaper(arxivId));
    }
}
