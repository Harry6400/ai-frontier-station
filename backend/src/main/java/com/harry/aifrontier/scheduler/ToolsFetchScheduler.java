package com.harry.aifrontier.scheduler;

import com.harry.aifrontier.service.ToolsRssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolsFetchScheduler {

    private final ToolsRssService toolsRssService;

    @Scheduled(cron = "0 0 12 * * *")
    public void scheduledFetch() {
        runOnce();
    }

    public void runOnce() {
        log.info("[Tools 定时采集] 开始执行...");
        try {
            int count = toolsRssService.fetchTools();
            log.info("[Tools 定时采集] 执行完毕: 新增 {} 条工具实践", count);
        } catch (Exception e) {
            log.error("[Tools 定时采集] 执行失败: {}", e.getMessage(), e);
        }
    }
}
