package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.entity.Source;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.mapper.SourceMapper;
import com.harry.aifrontier.service.AutoPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pipeline A: auto-publish candidates without admin review.
 * Used for github, tools, arena content types.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPublishServiceImpl implements AutoPublishService {

    private final ContentCandidateMapper candidateMapper;
    private final ContentMapper contentMapper;
    private final CategoryMapper categoryMapper;
    private final SourceMapper sourceMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishCandidate(Long candidateId) {
        ContentCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null || "imported".equals(candidate.getStatus())) {
            return;
        }
        doPublish(candidate);
    }

    @Override
    @Transactional
    public int publishPending(String sourceType) {
        List<ContentCandidate> pending = candidateMapper.selectList(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getSourceType, sourceType)
                        .eq(ContentCandidate::getStatus, "pending")
                        .orderByDesc(ContentCandidate::getCreatedAt));

        int count = 0;
        for (ContentCandidate candidate : pending) {
            try {
                doPublish(candidate);
                count++;
            } catch (Exception e) {
                log.warn("Auto-publish failed for candidate {}: {}", candidate.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("Auto-published {} {} candidates", count, sourceType);
        }
        return count;
    }

    private void doPublish(ContentCandidate candidate) {
        String sourceType = candidate.getSourceType();
        String contentType = mapContentType(sourceType);
        Long categoryId = findCategoryId(mapCategorySlug(sourceType));
        Long sourceId = findSourceId(mapSourceSlug(sourceType));

        String title = candidate.getTitle();
        String slug = generateSlug(title);
        String summary = candidate.getAiSummary();
        String bodyMarkdown = candidate.getAiBody();
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
            bodyMarkdown = candidate.getRawContent() != null ? candidate.getRawContent() : " ";
        }

        Content content = new Content();
        content.setTitle(title);
        content.setSlug(slug);
        content.setContentType(contentType);
        content.setSummary(summary);
        content.setBodyMarkdown(bodyMarkdown);
        content.setCategoryId(categoryId);
        content.setSourceId(sourceId);
        content.setSourceUrl(candidate.getUrl());
        content.setAuthorName(candidate.getAuthor());
        content.setPublishStatus("PUBLISHED");
        content.setPublishedAt(candidate.getPublishedAt() != null
                ? candidate.getPublishedAt() : LocalDateTime.now());
        content.setExtraJson(candidate.getMetadataJson());
        content.setTrendScore(candidate.getTrendScore());
        content.setStarGrowth7d(candidate.getStarGrowth7d());
        content.setForkCount(candidate.getForkCount());
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        content.setFeaturedLevel(0);
        content.setViewCount(0);
        content.setReadingTime(0);

        contentMapper.insert(content);

        candidate.setStatus("imported");
        candidate.setContentId(content.getId());
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);
    }

    private String mapContentType(String sourceType) {
        switch (sourceType) {
            case "github":  return "project";
            case "news":    return "news";
            case "product": return "company_update";
            case "arena":   return "arena";
            case "tools":   return "practice";
            default:        return "news";
        }
    }

    private String mapCategorySlug(String sourceType) {
        switch (sourceType) {
            case "github":  return "products";
            case "news":    return "news";
            case "product": return "products";
            case "arena":   return "arena";
            case "tools":   return "tools";
            default:        return "news";
        }
    }

    private String mapSourceSlug(String sourceType) {
        switch (sourceType) {
            case "github":  return "github-trending";
            case "news":    return "hacker-news";
            case "product": return "openai-blog";
            case "arena":   return "chatbot-arena";
            case "tools":   return "reddit";
            default:        return "github-trending";
        }
    }

    private Long findCategoryId(String slug) {
        Category cat = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getSlug, slug));
        return cat != null ? cat.getId() : null;
    }

    private Long findSourceId(String slug) {
        Source src = sourceMapper.selectOne(
                new LambdaQueryWrapper<Source>().eq(Source::getSlug, slug));
        return src != null ? src.getId() : null;
    }

    private String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
