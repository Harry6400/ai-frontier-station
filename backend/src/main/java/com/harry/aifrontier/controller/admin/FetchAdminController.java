package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.entity.FetchLog;
import com.harry.aifrontier.service.FetchLogService;
import com.harry.aifrontier.service.PaperSummaryService;
import com.harry.aifrontier.scheduler.ArxivFetchScheduler;
import com.harry.aifrontier.scheduler.HuggingFaceFetchScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/fetch")
@RequiredArgsConstructor
public class FetchAdminController {

    private final FetchLogService fetchLogService;
    private final PaperSummaryService paperSummaryService;

    /** 注入调度器（可能还未创建，允许为空） */
    @Autowired(required = false)
    private ArxivFetchScheduler arxivFetchScheduler;

    @Autowired(required = false)
    private HuggingFaceFetchScheduler huggingFaceFetchScheduler;

    /** GET /logs?days=7 — 查询最近 N 天的采集日志 */
    @GetMapping("/logs")
    public ApiResponse<List<FetchLog>> getLogs(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(fetchLogService.recentLogs(days));
    }

    /** GET /logs/source/{source}?days=7 — 按来源查日志 */
    @GetMapping("/logs/source/{source}")
    public ApiResponse<List<FetchLog>> getLogsBySource(
            @PathVariable String source,
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(fetchLogService.recentLogsBySource(source, days));
    }

    /** POST /trigger/arxiv — 手动触发 arXiv 采集 */
    @PostMapping("/trigger/arxiv")
    public ApiResponse<String> triggerArxiv() {
        if (arxivFetchScheduler == null) {
            return ApiResponse.fail(503, "ArxivFetchScheduler 尚未启用");
        }
        try {
            arxivFetchScheduler.runOnce();
            return ApiResponse.success("arXiv 采集已触发", "ok");
        } catch (Exception e) {
            return ApiResponse.fail(500, "arXiv 采集触发失败: " + e.getMessage());
        }
    }

    /** POST /trigger/huggingface — 手动触发 HuggingFace 采集 */
    @PostMapping("/trigger/huggingface")
    public ApiResponse<String> triggerHuggingFace() {
        if (huggingFaceFetchScheduler == null) {
            return ApiResponse.fail(503, "HuggingFaceFetchScheduler 尚未启用");
        }
        try {
            huggingFaceFetchScheduler.runOnce();
            return ApiResponse.success("HuggingFace 采集已触发", "ok");
        } catch (Exception e) {
            return ApiResponse.fail(500, "HuggingFace 采集触发失败: " + e.getMessage());
        }
    }

    /** POST /trigger/summary — 批量为所有已发布内容生成中文摘要 */
    @PostMapping("/trigger/summary")
    public ApiResponse<String> triggerBatchSummary() {
        try {
            String result = paperSummaryService.generateAllMissing();
            return ApiResponse.success(result, "ok");
        } catch (Exception e) {
            return ApiResponse.fail(500, "批量摘要生成失败: " + e.getMessage());
        }
    }
}
