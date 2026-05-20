package com.harry.aifrontier.scheduler;

import com.harry.aifrontier.service.AutoPublishService;
import com.harry.aifrontier.service.GitHubTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubWeeklyScheduler {

    private final GitHubTrendingService gitHubTrendingService;
    private final AutoPublishService autoPublishService;

    @Scheduled(cron = "0 0 9 */3 * *")
    public void weeklyFetch() {
        log.info("[GitHub Weekly] 开始每周Top 10刷新");
        try {
            // Fetch new trending repos (weekly)
            int count = gitHubTrendingService.fetchTrending("", "weekly");
            log.info("[GitHub Weekly] 获取 {} 个仓库", count);

            // Auto-publish pending GitHub candidates
            int published = autoPublishService.publishPending("github");
            log.info("[GitHub Weekly] 发布 {} 个项目", published);
        } catch (Exception e) {
            log.error("[GitHub Weekly] 失败: {}", e.getMessage(), e);
        }
    }
}
