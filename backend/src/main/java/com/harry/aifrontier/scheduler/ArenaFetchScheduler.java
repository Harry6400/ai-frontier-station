package com.harry.aifrontier.scheduler;

import com.harry.aifrontier.service.ArenaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArenaFetchScheduler {

    private final ArenaService arenaService;

    @Scheduled(cron = "0 0 10 * * *")
    public void scheduledFetch() {
        runOnce();
    }

    public void runOnce() {
        log.info("[Arena 定时采集] 开始执行...");
        try {
            int count = arenaService.fetchArenaData();
            log.info("[Arena 定时采集] 执行完毕: 新增 {} 条竞技场数据", count);
        } catch (Exception e) {
            log.error("[Arena 定时采集] 执行失败: {}", e.getMessage(), e);
        }
    }
}
