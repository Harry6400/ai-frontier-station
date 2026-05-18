package com.harry.aifrontier.controller;

import com.harry.aifrontier.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projectName", "AI前沿情报站");
        data.put("status", "UP");
        data.put("stage", "工程骨架初始化");
        data.put("date", LocalDate.now());
        return ApiResponse.success(data);
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("positioning", "AI内容精选与发布系统第一版");
        data.put("futureDirection", "AI信息聚合与精选平台");
        data.put("frontendApps", List.of("frontend-portal", "frontend-admin"));
        data.put("backendModules", List.of("content", "taxonomy", "source", "publish", "search"));
        data.put("currentScope", List.of("内容管理", "分类管理", "标签管理", "来源管理", "前台展示"));
        return ApiResponse.success(data);
    }
}

