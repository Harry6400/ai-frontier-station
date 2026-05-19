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
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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
    private final ApiCredentialService apiCredentialService;

    @Value("${app.bailian.api-key:}")
    private String bailianFallbackKey;

    @Value("${app.bailian.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String bailianBaseUrl;

    @Value("${app.bailian.model:qwen-turbo}")
    private String bailianModel;

    @Override
    public PageResult<EventListItemVO> listEvents(Integer pageNum, Integer pageSize) {
        Page<ContentEvent> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ContentEvent> wrapper = new LambdaQueryWrapper<ContentEvent>()
                .orderByDesc(ContentEvent::getCreatedAt);

        Page<ContentEvent> result = eventMapper.selectPage(page, wrapper);

        List<EventListItemVO> records = result.getRecords().stream()
                .map(this::toListVO)
                .toList();

        return PageResult.of(result.getTotal(), pageNum.longValue(), pageSize.longValue(), records);
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
        // AI-powered clustering: send content titles to LLM and ask it to group them
        List<Content> allContents = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getPublishStatus, "PUBLISHED")
                        .orderByDesc(Content::getCreatedAt)
        );

        if (allContents.isEmpty()) return 0;

        // Skip already linked contents
        Set<Long> linkedContentIds = linkMapper.selectList(null)
                .stream()
                .map(ContentEventLink::getContentId)
                .collect(Collectors.toSet());

        List<Content> unlinkedContents = allContents.stream()
                .filter(c -> !linkedContentIds.contains(c.getId()))
                .toList();

        if (unlinkedContents.size() < 2) {
            log.info("Not enough unlinked contents for clustering (need >= 2, have {})", unlinkedContents.size());
            return 0;
        }

        // Build a numbered list of content titles for the AI
        StringBuilder contentList = new StringBuilder();
        for (int i = 0; i < unlinkedContents.size(); i++) {
            Content c = unlinkedContents.get(i);
            contentList.append(String.format("%d. [ID:%d] %s\n", i + 1, c.getId(),
                    c.getTitle().length() > 100 ? c.getTitle().substring(0, 100) + "..." : c.getTitle()));
        }

        String apiKey = apiCredentialService.resolveBailianApiKey(bailianFallbackKey);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("百炼 API Key 未配置，无法执行AI聚类");
            return 0;
        }

        String prompt = "你是AI前沿情报站的内容分析助手。下面是一批AI领域的文章标题，请将它们按主题聚类成事件。\n\n" +
                "规则：\n" +
                "1. 只聚类确实相关的文章（同一公司/产品/技术方向）\n" +
                "2. 不要强行聚类，如果某篇文章不属于任何事件，跳过它\n" +
                "3. 每个事件至少包含2篇文章\n" +
                "4. 用中文生成事件标题\n\n" +
                "文章列表：\n" + contentList +
                "\n请严格以JSON数组格式输出，不要添加其他内容：\n" +
                "[{\"title\": \"事件标题\", \"contentIds\": [1, 3, 5]}, ...]";

        try {
            Map<String, Object> body = Map.of(
                    "model", bailianModel,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.1,
                    "max_tokens", 2000
            );

            String response = RestClient.create(bailianBaseUrl)
                    .post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            // Parse response
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            // Extract JSON from response (handle markdown code blocks)
            String json = content.replaceAll("```json\\s*", "").replaceAll("```", "").trim();

            com.fasterxml.jackson.databind.JsonNode clusters = objectMapper.readTree(json);
            if (!clusters.isArray()) {
                log.warn("AI clustering response is not an array: {}", json.substring(0, Math.min(200, json.length())));
                return 0;
            }

            // Build content ID lookup map
            Map<Long, Content> contentMap = unlinkedContents.stream()
                    .collect(Collectors.toMap(Content::getId, c -> c));

            List<ContentEvent> existingEvents = eventMapper.selectList(null);
            int newEventCount = 0;

            for (com.fasterxml.jackson.databind.JsonNode cluster : clusters) {
                String eventTitle = cluster.path("title").asText("未命名事件");
                com.fasterxml.jackson.databind.JsonNode idsNode = cluster.path("contentIds");

                if (!idsNode.isArray() || idsNode.size() < 2) continue;

                List<Long> contentIds = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode idNode : idsNode) {
                    // AI returns 1-based indices, convert to actual content IDs
                    int idx = idNode.asInt() - 1;
                    if (idx >= 0 && idx < unlinkedContents.size()) {
                        contentIds.add(unlinkedContents.get(idx).getId());
                    }
                }

                if (contentIds.size() < 2) continue;

                // Check if an existing event with similar title exists
                ContentEvent matchedEvent = null;
                Set<String> eventWords = extractKeywords(eventTitle);
                for (ContentEvent existing : existingEvents) {
                    Set<String> existingWords = extractKeywords(existing.getTitle());
                    Set<String> overlap = new HashSet<>(eventWords);
                    overlap.retainAll(existingWords);
                    if (overlap.size() >= 1) {
                        matchedEvent = existing;
                        break;
                    }
                }

                if (matchedEvent != null) {
                    // Merge into existing event
                    for (Long cid : contentIds) {
                        linkContent(matchedEvent.getId(), cid);
                    }
                    matchedEvent.setContentCount(matchedEvent.getContentCount() + contentIds.size());
                    eventMapper.updateById(matchedEvent);
                } else {
                    // Create new event
                    ContentEvent event = new ContentEvent();
                    event.setTitle(eventTitle);
                    event.setSummary("AI自动聚类");
                    event.setContentCount(contentIds.size());
                    event.setViewCount(0);
                    eventMapper.insert(event);

                    for (Long cid : contentIds) {
                        linkContent(event.getId(), cid);
                    }
                    existingEvents.add(event);
                    newEventCount++;
                }
            }

            log.info("AI auto-cluster completed: {} new events created from {} contents", newEventCount, unlinkedContents.size());
            return newEventCount;

        } catch (Exception e) {
            log.error("AI clustering failed: {}", e.getMessage(), e);
            // Fall back to keyword-based clustering
            return keywordCluster(unlinkedContents);
        }
    }

    /**
     * Fallback keyword-based clustering when AI is unavailable
     */
    private int keywordCluster(List<Content> unlinkedContents) {
        Set<Long> processed = new HashSet<>();
        List<ContentEvent> existingEvents = eventMapper.selectList(null);
        int newEventCount = 0;

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
                Set<String> overlap = new HashSet<>(baseKeywords);
                overlap.retainAll(candidateKeywords);

                if (overlap.size() >= 2) {
                    clusterIds.add(candidate.getId());
                    processed.add(candidate.getId());
                }
            }

            if (clusterIds.size() >= 2) {
                ContentEvent event = new ContentEvent();
                event.setTitle(generateEventTitle(base));
                event.setSummary("关键词聚类（AI不可用）");
                event.setContentCount(clusterIds.size());
                event.setViewCount(0);
                eventMapper.insert(event);

                for (Long cid : clusterIds) {
                    linkContent(event.getId(), cid);
                }
                newEventCount++;
            }
        }

        log.info("Keyword fallback cluster completed: {} new events", newEventCount);
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
