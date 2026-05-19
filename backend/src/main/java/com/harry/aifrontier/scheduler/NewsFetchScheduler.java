package com.harry.aifrontier.scheduler;

import com.harry.aifrontier.service.NewsRssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsFetchScheduler {

    private final NewsRssService newsRssService;

    @Scheduled(cron = "0 0 8,20 * * *")
    public void scheduledFetch() {
        runOnce();
    }

    public void runOnce() {
        log.info("[News 定时采集] 开始执行...");
        try {
            int count = newsRssService.fetchNews();
            log.info("[News 定时采集] 执行完毕: 新增 {} 条新闻", count);
        } catch (Exception e) {
            log.error("[News 定时采集] 执行失败: {}", e.getMessage(), e);
        }
    }
}
