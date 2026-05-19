package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.service.ArenaService;
import com.harry.aifrontier.util.CandidateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaServiceImpl implements ArenaService {

    private final ContentCandidateMapper contentCandidateMapper;
    private final ObjectMapper objectMapper;

    private static final String ARENA_URL = "https://leaderboard.lmsys.org/";
    private static final String ARENA_EXTERNAL_ID = "lmsys_arena_leaderboard";

    @Override
    public int fetchArenaData() {
        log.info("开始获取 LMSYS Arena 排行榜数据...");

        try {
            // LMSYS Arena 是一个 React SPA，大部分数据通过 JS 动态加载
            // 尝试获取页面内容，如果有可用的 API 端点则使用
            String content = tryFetchArenaPage();

            if (content != null && !content.isBlank()) {
                // 尝试从页面提取基本排行榜信息
                return processArenaContent(content);
            }

            // 如果无法获取实际数据，创建一个占位候选记录，标记需要手动更新
            return createManualEntryCandidate();
        } catch (RestClientException e) {
            log.error("Arena 页面请求失败: {}", e.getMessage());
            return createManualEntryCandidate();
        } catch (Exception e) {
            log.error("Arena 数据获取异常: {}", e.getMessage(), e);
            return createManualEntryCandidate();
        }
    }

    private String tryFetchArenaPage() {
        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://leaderboard.lmsys.org")
                    .defaultHeader("Accept", "text/html,application/xhtml+xml")
                    .defaultHeader("User-Agent", "AI-Frontier-Station/1.0 (Educational Project)")
                    .build();

            String html = client.get().uri("/").retrieve().body(String.class);

            if (html != null && !html.isBlank()) {
                log.info("成功获取 Arena 页面 HTML，长度: {}", html.length());
                return html;
            }
            return null;
        } catch (Exception e) {
            log.warn("Arena 页面获取失败: {}", e.getMessage());
            return null;
        }
    }

    private int processArenaContent(String html) {
        // 由于 LMSYS Arena 是 SPA，HTML 内容大部分是 JS bundle
        // 尝试从 HTML 中提取 meta 信息或初始数据
        String title = "LMSYS Chatbot Arena Leaderboard";
        String description = "Chatbot Arena 排行榜 - 基于人类偏好的 LLM 评估排名";

        // 检查是否有可以解析的数据片段
        if (html.contains("\"rank\"") || html.contains("\"score\"") || html.contains("\"model\"")) {
            log.info("Arena 页面可能包含排行榜数据，但需要 JS 渲染，标记为手动更新");
        }

        if (existsByExternalId(ARENA_EXTERNAL_ID)) {
            log.info("Arena 排行榜记录已存在，更新状态");
            updateExistingCandidate(ARENA_EXTERNAL_ID);
            return 0;
        }

        ContentCandidate candidate = new ContentCandidate();
        candidate.setSourceType("arena");
        candidate.setExternalId(ARENA_EXTERNAL_ID);
        candidate.setTitle(title);
        candidate.setUrl(ARENA_URL);
        candidate.setRawContent(description);
        candidate.setAuthor("LMSYS");
        candidate.setPublishedAt(LocalDateTime.now());
        candidate.setStatus("pending");

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "lmsys");
            metadata.put("type", "leaderboard");
            metadata.put("note", "SPA 页面，需要 JS 渲染获取完整数据，建议手动更新排行榜排名");
            candidate.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            candidate.setMetadataJson("{}");
        }

        if (CandidateValidator.validate(candidate)) {
            contentCandidateMapper.insert(candidate);
            log.info("新增 Arena 排行榜占位记录");
            return 1;
        } else {
            log.debug("数据质量验证未通过，跳过 Arena 占位记录");
            return 0;
        }
    }

    private int createManualEntryCandidate() {
        if (existsByExternalId(ARENA_EXTERNAL_ID)) {
            log.info("Arena 排行榜记录已存在，跳过");
            return 0;
        }

        ContentCandidate candidate = new ContentCandidate();
        candidate.setSourceType("arena");
        candidate.setExternalId(ARENA_EXTERNAL_ID);
        candidate.setTitle("LMSYS Chatbot Arena Leaderboard");
        candidate.setUrl(ARENA_URL);
        candidate.setRawContent("Chatbot Arena 排行榜基于人类偏好评估 LLM 能力。" +
                "由于页面为动态渲染（React SPA），自动抓取受限，建议手动更新最新排名数据。");
        candidate.setAuthor("LMSYS");
        candidate.setPublishedAt(LocalDateTime.now());
        candidate.setStatus("pending");

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "lmsys");
            metadata.put("type", "leaderboard");
            metadata.put("manualUpdate", true);
            metadata.put("note", "自动抓取失败或数据不完整，需要手动更新排行榜排名");
            candidate.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            candidate.setMetadataJson("{}");
        }

        if (CandidateValidator.validate(candidate)) {
            contentCandidateMapper.insert(candidate);
            log.warn("新增 Arena 排行榜手动更新占位记录");
            return 1;
        } else {
            log.debug("数据质量验证未通过，跳过 Arena 手动占位记录");
            return 0;
        }
    }

    private boolean existsByExternalId(String externalId) {
        return contentCandidateMapper.selectCount(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getExternalId, externalId)
                        .eq(ContentCandidate::getSourceType, "arena")
        ) > 0;
    }

    private void updateExistingCandidate(String externalId) {
        try {
            ContentCandidate existing = contentCandidateMapper.selectOne(
                    new LambdaQueryWrapper<ContentCandidate>()
                            .eq(ContentCandidate::getExternalId, externalId)
                            .eq(ContentCandidate::getSourceType, "arena")
                            .last("LIMIT 1")
            );
            if (existing != null) {
                existing.setUpdatedAt(LocalDateTime.now());
                contentCandidateMapper.updateById(existing);
                log.info("更新 Arena 记录时间戳");
            }
        } catch (Exception e) {
            log.warn("更新 Arena 记录失败: {}", e.getMessage());
        }
    }
}
