package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.service.HuggingFaceService;
import com.harry.aifrontier.vo.HuggingFacePaperCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/huggingface")
@RequiredArgsConstructor
public class HuggingFaceAdminController {

    private final HuggingFaceService huggingFaceService;

    @GetMapping("/papers")
    public ApiResponse<List<HuggingFacePaperCandidateVO>> getDailyPapers() {
        return ApiResponse.success(huggingFaceService.getDailyPapers());
    }

    @GetMapping("/papers/{paperId}")
    public ApiResponse<HuggingFacePaperCandidateVO> findPaper(@PathVariable String paperId) {
        return ApiResponse.success(huggingFaceService.findPaper(paperId));
    }
}
