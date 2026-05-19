package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.EventSaveRequest;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentEvent;
import com.harry.aifrontier.entity.ContentEventLink;
import com.harry.aifrontier.mapper.ContentEventLinkMapper;
import com.harry.aifrontier.mapper.ContentEventMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.service.EventService;
import com.harry.aifrontier.vo.EventContentVO;
import com.harry.aifrontier.vo.EventDetailVO;
import com.harry.aifrontier.vo.EventListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final ContentEventMapper eventMapper;
    private final ContentEventLinkMapper linkMapper;
    private final ContentMapper contentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<EventListItemVO> listEvents(Integer pageNum, Integer pageSize) {
        Page<ContentEvent> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ContentEvent> wrapper = new LambdaQueryWrapper<ContentEvent>()
                .orderByDesc(ContentEvent::getCreatedAt);

        Page<ContentEvent> result = eventMapper.selectPage(page, wrapper);

        List<EventListItemVO> records = result.getRecords().stream()
                .map(this::toListVO)
                .toList();

        return PageResult.of(result.getTotal(), pageNum, pageSize, records);
    }

    @Override
    public EventDetailVO getEvent(Long id) {
        ContentEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new IllegalArgumentException("事件不存在");
        }

        // Increment view count
        event.setViewCount(event.getViewCount() + 1);
        eventMapper.updateById(event);

        return toDetailVO(event);
    }

    @Override
    @Transactional
    public EventDetailVO createEvent(EventSaveRequest request) {
        // Create the event
        ContentEvent event = new ContentEvent();
        event.setTitle(request.getTitle().trim());
        event.setSummary(request.getSummary());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setTags(request.getTags());
        event.setContentCount(request.getContentIds().size());
        event.setViewCount(0);
        eventMapper.insert(event);

        // Link contents
        for (Long contentId : request.getContentIds()) {
            linkContent(event.getId(), contentId);
        }

        return toDetailVO(event);
    }

    @Override
    @Transactional
    public EventDetailVO updateEvent(Long id, EventSaveRequest request) {
        ContentEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new IllegalArgumentException("事件不存在");
        }

        event.setTitle(request.getTitle().trim());
        event.setSummary(request.getSummary());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setTags(request.getTags());

        // Remove old links and re-link
        linkMapper.delete(new LambdaQueryWrapper<ContentEventLink>()
                .eq(ContentEventLink::getEventId, id));

        for (Long contentId : request.getContentIds()) {
            linkContent(id, contentId);
        }

        event.setContentCount(request.getContentIds().size());
        eventMapper.updateById(event);

        return toDetailVO(event);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        ContentEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new IllegalArgumentException("事件不存在");
        }

        // Delete links first
        linkMapper.delete(new LambdaQueryWrapper<ContentEventLink>()
                .eq(ContentEventLink::getEventId, id));

        // Delete event
        eventMapper.deleteById(id);
    }

    @Override
    @Transactional
    public int autoCluster() {
        // Simple keyword-based clustering
        // Get all published contents that are not yet linked to any event
        List<Content> allContents = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getPublishStatus, "PUBLISHED")
                        .isNotNull(Content::getAiTags)
                        .orderByDesc(Content::getCreatedAt)
        );

        // Get existing content-event links to skip already linked content
        Set<Long> linkedContentIds = linkMapper.selectList(null)
                .stream()
                .map(ContentEventLink::getContentId)
                .collect(Collectors.toSet());

        // Get existing events for matching
        List<ContentEvent> existingEvents = eventMapper.selectList(null);

        int newEventCount = 0;

        // Group contents by overlapping keywords in title/tags
        List<Content> unlinkedContents = allContents.stream()
                .filter(c -> !linkedContentIds.contains(c.getId()))
                .toList();

        // Simple approach: find contents with shared significant words in title
        Set<Long> processed = new HashSet<>();
        for (int i = 0; i < unlinkedContents.size(); i++) {
            if (processed.contains(unlinkedContents.get(i).getId())) continue;

            Content base = unlinkedContents.get(i);
            Set<String> baseKeywords = extractKeywords(base.getTitle());
            if (baseKeywords.isEmpty()) continue;

            List<Long> clusterIds = new ArrayList<>();
            clusterIds.add(base.getId());
            processed.add(base.getId());

            for (int j = i + 1; j < unlinkedContents.size(); j++) {
                Content candidate = unlinkedContents.get(j);
                if (processed.contains(candidate.getId())) continue;

                Set<String> candidateKeywords = extractKeywords(candidate.getTitle());
                if (candidateKeywords.isEmpty()) continue;

                // Check for keyword overlap
                Set<String> overlap = new HashSet<>(baseKeywords);
                overlap.retainAll(candidateKeywords);

                if (overlap.size() >= 2) {
                    clusterIds.add(candidate.getId());
                    processed.add(candidate.getId());
                }
            }

            // Only create event if we found a cluster of 2+ contents
            if (clusterIds.size() >= 2) {
                // Check if any existing event matches these keywords
                boolean merged = false;
                for (ContentEvent existing : existingEvents) {
                    Set<String> eventKeywords = extractKeywords(existing.getTitle());
                    Set<String> overlap = new HashSet<>(eventKeywords);
                    overlap.retainAll(baseKeywords);
                    if (overlap.size() >= 2) {
                        // Add to existing event
                        for (Long cid : clusterIds) {
                            linkContent(existing.getId(), cid);
                        }
                        existing.setContentCount(existing.getContentCount() + clusterIds.size());
                        eventMapper.updateById(existing);
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    // Create new event
                    ContentEvent event = new ContentEvent();
                    event.setTitle(generateEventTitle(base));
                    event.setSummary("自动聚类生成");
                    event.setContentCount(clusterIds.size());
                    event.setViewCount(0);
                    eventMapper.insert(event);

                    for (Long cid : clusterIds) {
                        linkContent(event.getId(), cid);
                    }
                    existingEvents.add(event);
                    newEventCount++;
                }
            }
        }

        log.info("Auto-cluster completed: {} new events created", newEventCount);
        return newEventCount;
    }

    private void linkContent(Long eventId, Long contentId) {
        // Check if link already exists
        Long count = linkMapper.selectCount(new LambdaQueryWrapper<ContentEventLink>()
                .eq(ContentEventLink::getEventId, eventId)
                .eq(ContentEventLink::getContentId, contentId));
        if (count > 0) return;

        ContentEventLink link = new ContentEventLink();
        link.setEventId(eventId);
        link.setContentId(contentId);
        linkMapper.insert(link);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();

        // Split by common separators and filter short/stop words
        Set<String> stopWords = Set.of("the", "a", "an", "is", "are", "was", "were",
                "in", "on", "at", "to", "for", "of", "with", "by", "and", "or", "but",
                "not", "this", "that", "it", "its", "new", "how", "what", "why",
                "的", "了", "在", "是", "和", "与", "对", "被", "将", "从", "到",
                "一", "不", "也", "有", "这", "那", "个", "中", "大", "小");

        return Arrays.stream(text.toLowerCase()
                        .split("[\\s,;.!?，。；！？\\-_/()（）\\[\\]【】]+"))
                .filter(w -> w.length() >= 2)
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.toSet());
    }

    private String generateEventTitle(Content base) {
        // Use first significant part of title as event title
        String title = base.getTitle();
        if (title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }
        return title;
    }

    private EventListItemVO toListVO(ContentEvent event) {
        EventListItemVO vo = new EventListItemVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setSummary(event.getSummary());
        vo.setEventDate(event.getEventDate());
        vo.setTags(parseTags(event.getTags()));
        vo.setContentCount(event.getContentCount());
        vo.setViewCount(event.getViewCount());
        vo.setCreatedAt(event.getCreatedAt());
        vo.setUpdatedAt(event.getUpdatedAt());
        return vo;
    }

    private EventDetailVO toDetailVO(ContentEvent event) {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setSummary(event.getSummary());
        vo.setDescription(event.getDescription());
        vo.setEventDate(event.getEventDate());
        vo.setTags(parseTags(event.getTags()));
        vo.setContentCount(event.getContentCount());
        vo.setViewCount(event.getViewCount());
        vo.setCreatedAt(event.getCreatedAt());
        vo.setUpdatedAt(event.getUpdatedAt());

        // Load linked contents
        List<ContentEventLink> links = linkMapper.selectList(
                new LambdaQueryWrapper<ContentEventLink>()
                        .eq(ContentEventLink::getEventId, event.getId()));

        List<EventContentVO> contents = new ArrayList<>();
        for (ContentEventLink link : links) {
            Content content = contentMapper.selectById(link.getContentId());
            if (content != null) {
                EventContentVO contentVO = new EventContentVO();
                contentVO.setContentId(content.getId());
                contentVO.setLinkId(link.getId());
                contentVO.setTitle(content.getTitle());
                contentVO.setContentType(content.getContentType());
                contentVO.setSummary(content.getSummary());
                contentVO.setCoverImage(content.getCoverImage());
                contentVO.setSourceUrl(content.getSourceUrl());
                contentVO.setAuthorName(content.getAuthorName());
                contentVO.setPublishStatus(content.getPublishStatus());
                contents.add(contentVO);
            }
        }
        vo.setContents(contents);

        return vo;
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
