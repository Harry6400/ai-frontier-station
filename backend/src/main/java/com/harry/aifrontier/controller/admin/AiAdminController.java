package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.dto.request.AiSourceSummaryRequest;
import com.harry.aifrontier.service.AiSummaryService;
import com.harry.aifrontier.vo.AiSourceSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiSummaryService aiSummaryService;

    @PostMapping("/source-summary")
    public ApiResponse<AiSourceSummaryVO> summarizeSource(@Valid @RequestBody AiSourceSummaryRequest request) {
        return ApiResponse.success("AI 导读生成成功", aiSummaryService.summarizeSource(request));
    }
}
