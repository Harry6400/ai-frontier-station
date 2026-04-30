package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentExternalRef;
import com.harry.aifrontier.entity.Source;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentExternalRefMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.mapper.SourceMapper;
import com.harry.aifrontier.mapper.TagMapper;
import com.harry.aifrontier.service.DashboardService;
import com.harry.aifrontier.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Map<String, String> PUBLISH_STATUS_LABELS = Map.of(
            "DRAFT", "草稿",
            "PUBLISHED", "已发布",
            "ARCHIVED", "已归档"
    );

    private static final Map<String, String> CONTENT_TYPE_LABELS = Map.of(
            "news", "资讯",
            "paper", "论文",
            "project", "项目",
            "company_update", "公司动态",
            "practice", "技术实践"
    );

    private static final Map<String, String> SOURCE_TYPE_LABELS = Map.of(
            "github", "GitHub",
            "paper", "论文站点",
            "official_blog", "官方博客",
            "community", "科技社区",
            "manual", "人工录入"
    );

    private final ContentMapper contentMapper;
    private final CategoryMapper categoryMapper;
    private final SourceMapper sourceMapper;
    private final TagMapper tagMapper;
    private final ContentExternalRefMapper contentExternalRefMapper;

    @Override
    public DashboardOverviewVO overview() {
        List<Content> contents = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .orderByDesc(Content::getUpdatedAt)
                .orderByDesc(Content::getId));
        List<Source> sources = sourceMapper.selectList(new LambdaQueryWrapper<Source>()
                .orderByDesc(Source::getId));
        List<ContentExternalRef> externalRefs = contentExternalRefMapper.selectList(new LambdaQueryWrapper<ContentExternalRef>()
                .orderByDesc(ContentExternalRef::getSyncedAt)
                .orderByDesc(ContentExternalRef::getId));

        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setTotals(buildTotals(contents, sources, externalRefs));
        vo.setPublishStatusStats(buildStats(contents, Content::getPublishStatus, PUBLISH_STATUS_LABELS, List.of("PUBLISHED", "DRAFT", "ARCHIVED")));
        vo.setContentTypeStats(buildStats(contents, Content::getContentType, CONTENT_TYPE_LABELS, List.of("project", "news", "paper", "company_update", "practice")));
        vo.setSourceTypeStats(buildStats(sources, Source::getSourceType, SOURCE_TYPE_LABELS, List.of("github", "paper", "official_blog", "community", "manual")));
        vo.setRecentContents(buildRecentContents(contents.stream().limit(6).toList()));
        vo.setRecentExternalRefs(buildRecentExternalRefs(externalRefs.stream().limit(5).toList()));
        return vo;
    }

    private DashboardOverviewVO.TotalsVO buildTotals(List<Content> contents, List<Source> sources, List<ContentExternalRef> externalRefs) {
        DashboardOverviewVO.TotalsVO totals = new DashboardOverviewVO.TotalsVO();
        totals.setContents((long) contents.size());
        totals.setPublished(countByValue(contents, Content::getPublishStatus, "PUBLISHED"));
        totals.setDrafts(countByValue(contents, Content::getPublishStatus, "DRAFT"));
        totals.setArchived(countByValue(contents, Content::getPublishStatus, "ARCHIVED"));
        totals.setCategories(categoryMapper.selectCount(null));
        totals.setTags(tagMapper.selectCount(null));
        totals.setSources((long) sources.size());
        totals.setExternalRefs((long) externalRefs.size());
        return totals;
    }

    private <T> List<DashboardOverviewVO.StatItemVO> buildStats(
            List<T> items,
            Function<T, String> classifier,
            Map<String, String> labels,
            List<String> order
    ) {
        Map<String, Long> countMap = items.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        long total = items.size();
        return order.stream()
                .map(value -> toStatItem(value, labels.getOrDefault(value, value), countMap.getOrDefault(value, 0L), total))
                .toList();
    }

    private DashboardOverviewVO.StatItemVO toStatItem(String value, String label, Long count, long total) {
        DashboardOverviewVO.StatItemVO item = new DashboardOverviewVO.StatItemVO();
        item.setLabel(label);
        item.setValue(value);
        item.setCount(count);
        item.setRatio(total == 0 ? 0 : (int) Math.round(count * 100.0 / total));
        return item;
    }

    private Long countByValue(List<Content> contents, Function<Content, String> classifier, String value) {
        return contents.stream().filter(content -> Objects.equals(classifier.apply(content), value)).count();
    }

    private List<DashboardOverviewVO.RecentContentVO> buildRecentContents(List<Content> contents) {
        if (contents.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> categoryIds = contents.stream()
                .map(Content::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> sourceIds = contents.stream()
                .map(Content::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, Source> sourceMap = sourceIds.isEmpty() ? Collections.emptyMap() : sourceMapper.selectBatchIds(sourceIds).stream()
                .collect(Collectors.toMap(Source::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return contents.stream()
                .map(content -> toRecentContent(content, categoryMap, sourceMap))
                .toList();
    }

    private DashboardOverviewVO.RecentContentVO toRecentContent(Content content, Map<Long, Category> categoryMap, Map<Long, Source> sourceMap) {
        DashboardOverviewVO.RecentContentVO vo = new DashboardOverviewVO.RecentContentVO();
        vo.setId(content.getId());
        vo.setTitle(content.getTitle());
        vo.setContentType(content.getContentType());
        vo.setPublishStatus(content.getPublishStatus());
        vo.setViewCount(content.getViewCount());
        vo.setPublishedAt(content.getPublishedAt());
        vo.setUpdatedAt(content.getUpdatedAt());
        Category category = categoryMap.get(content.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        Source source = sourceMap.get(content.getSourceId());
        if (source != null) {
            vo.setSourceName(source.getName());
            vo.setSourceType(source.getSourceType());
        }
        return vo;
    }

    private List<DashboardOverviewVO.RecentExternalRefVO> buildRecentExternalRefs(List<ContentExternalRef> refs) {
        if (refs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> contentIds = refs.stream()
                .map(ContentExternalRef::getContentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Content> contentMap = contentIds.isEmpty() ? Collections.emptyMap() : contentMapper.selectBatchIds(contentIds).stream()
                .collect(Collectors.toMap(Content::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return refs.stream()
                .map(ref -> toRecentExternalRef(ref, contentMap))
                .toList();
    }

    private DashboardOverviewVO.RecentExternalRefVO toRecentExternalRef(ContentExternalRef ref, Map<Long, Content> contentMap) {
        DashboardOverviewVO.RecentExternalRefVO vo = new DashboardOverviewVO.RecentExternalRefVO();
        vo.setId(ref.getId());
        vo.setContentId(ref.getContentId());
        Content content = contentMap.get(ref.getContentId());
        vo.setContentTitle(content == null ? "关联内容已删除" : content.getTitle());
        vo.setRefType(ref.getRefType());
        vo.setExternalId(ref.getExternalId());
        vo.setExternalUrl(ref.getExternalUrl());
        vo.setSyncedAt(ref.getSyncedAt());
        return vo;
    }
}
