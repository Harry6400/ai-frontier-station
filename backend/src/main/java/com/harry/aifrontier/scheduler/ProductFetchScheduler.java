package com.harry.aifrontier.scheduler;

import com.harry.aifrontier.service.ProductUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFetchScheduler {

    private final ProductUpdateService productUpdateService;

    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledFetch() {
        runOnce();
    }

    public void runOnce() {
        log.info("[Product 定时采集] 开始执行...");
        try {
            int count = productUpdateService.fetchProductUpdates();
            log.info("[Product 定时采集] 执行完毕: 新增 {} 条产品动态", count);
        } catch (Exception e) {
            log.error("[Product 定时采集] 执行失败: {}", e.getMessage(), e);
        }
    }
}
