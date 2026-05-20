package com.harry.aifrontier.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.service.CandidateService;
import com.harry.aifrontier.service.AutoPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/candidates")
@RequiredArgsConstructor
public class CandidateAdminController {

    private final CandidateService candidateService;
    private final AutoPublishService autoPublishService;

    /**
     * List candidates with optional filters
     */
    @GetMapping
    public ApiResponse<PageResult<ContentCandidate>> list(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false, defaultValue = "pending") String status,
            @RequestParam(required = false, defaultValue = "1") Long pageNum,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {

        Page<ContentCandidate> page = candidateService.listCandidates(sourceType, status, pageNum, pageSize);
        return ApiResponse.success(PageResult.of(
                page.getTotal(), pageNum, pageSize, page.getRecords()));
    }

    /**
     * Get single candidate detail
     */
    @GetMapping("/{id}")
    public ApiResponse<ContentCandidate> detail(@PathVariable Long id) {
        return ApiResponse.success(candidateService.getCandidate(id));
    }

    /**
     * Update candidate fields
     */
    @PutMapping("/{id}")
    public ApiResponse<ContentCandidate> update(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        ContentCandidate candidate = candidateService.updateCandidate(id, body);
        return ApiResponse.success("候选内容更新成功", candidate);
    }

    /**
     * Approve and import candidate into Content table
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Long> approve(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> overrides) {
        Long contentId = candidateService.approveCandidate(id, overrides);
        return ApiResponse.success("审批导入成功", contentId);
    }

    /**
     * Reject candidate
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        candidateService.rejectCandidate(id);
        return ApiResponse.success("已驳回", null);
    }

    /**
     * AI-process a candidate: send to AI API and generate structured content
     */
    @PostMapping("/{id}/ai-process")
    public ApiResponse<ContentCandidate> aiProcess(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, String> body) {
        String customPrompt = (body != null) ? body.get("prompt") : null;
        ContentCandidate candidate = candidateService.aiProcess(id, customPrompt);
        return ApiResponse.success("AI 处理完成", candidate);
    }

    /**
     * Trigger manual fetch for a source type
     */
    @PostMapping("/fetch/{sourceType}")
    public ApiResponse<Integer> fetch(@PathVariable String sourceType) {
        int count = candidateService.fetchFromSource(sourceType);
        return ApiResponse.success("获取完成，新增 " + count + " 条候选", count);
    }

    /**
     * Get saved custom AI prompt
     */
    @GetMapping("/prompt")
    public ApiResponse<String> getPrompt() {
        return ApiResponse.success(candidateService.getCustomPrompt());
    }

    /**
     * Save custom AI prompt
     */
    @PutMapping("/prompt")
    public ApiResponse<Void> savePrompt(@RequestBody Map<String, String> body) {
        candidateService.saveCustomPrompt(body.get("prompt"));
        return ApiResponse.success(null);
    }

    @PostMapping("/publish/{sourceType}")
    public ApiResponse<Integer> publishPending(@PathVariable String sourceType) {
        int count = autoPublishService.publishPending(sourceType);
        return ApiResponse.success("发布完成，处理 " + count + " 条", count);
    }
}
