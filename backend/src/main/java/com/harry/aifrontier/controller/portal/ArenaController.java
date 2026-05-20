package com.harry.aifrontier.controller.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.entity.ArenaAnalysis;
import com.harry.aifrontier.entity.ArenaHistory;
import com.harry.aifrontier.entity.ArenaModel;
import com.harry.aifrontier.mapper.ArenaAnalysisMapper;
import com.harry.aifrontier.mapper.ArenaHistoryMapper;
import com.harry.aifrontier.mapper.ArenaModelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/arena")
@RequiredArgsConstructor
public class ArenaController {

    private final ArenaModelMapper arenaModelMapper;
    private final ArenaHistoryMapper arenaHistoryMapper;
    private final ArenaAnalysisMapper arenaAnalysisMapper;

    /**
     * 获取模型排行榜 (按 currentRank 升序)
     */
    @GetMapping("/ranking")
    public ApiResponse<List<ArenaModel>> ranking() {
        List<ArenaModel> list = arenaModelMapper.selectList(
                new LambdaQueryWrapper<ArenaModel>()
                        .orderByAsc(ArenaModel::getCurrentRank)
        );
        return ApiResponse.success(list);
    }

    /**
     * 获取指定模型最近30天的趋势数据
     */
    @GetMapping("/trend")
    public ApiResponse<List<ArenaHistory>> trend(@RequestParam Long modelId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<ArenaHistory> list = arenaHistoryMapper.selectList(
                new LambdaQueryWrapper<ArenaHistory>()
                        .eq(ArenaHistory::getModelId, modelId)
                        .ge(ArenaHistory::getRecordedAt, since)
                        .orderByAsc(ArenaHistory::getRecordedAt)
        );
        return ApiResponse.success(list);
    }

    /**
     * 获取最新一篇 Arena 分析报告
     */
    @GetMapping("/analysis")
    public ApiResponse<ArenaAnalysis> analysis() {
        ArenaAnalysis latest = arenaAnalysisMapper.selectOne(
                new LambdaQueryWrapper<ArenaAnalysis>()
                        .orderByDesc(ArenaAnalysis::getCreatedAt)
                        .last("LIMIT 1")
        );
        return ApiResponse.success(latest);
    }
}
