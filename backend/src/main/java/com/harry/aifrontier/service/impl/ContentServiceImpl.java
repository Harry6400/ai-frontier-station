package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.AiSourceImportRequest;
import com.harry.aifrontier.dto.request.ArxivPaperImportRequest;
import com.harry.aifrontier.dto.request.ContentQueryRequest;
import com.harry.aifrontier.dto.request.ContentExternalRefSaveRequest;
import com.harry.aifrontier.dto.request.ContentSaveRequest;
import com.harry.aifrontier.dto.request.GitHubRepoImportRequest;
import com.harry.aifrontier.dto.request.HuggingFacePaperImportRequest;
import com.harry.aifrontier.dto.request.PortalContentQueryRequest;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentExternalRef;
import com.harry.aifrontier.entity.ContentTag;
import com.harry.aifrontier.entity.Source;
import com.harry.aifrontier.entity.Tag;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.mapper.ContentExternalRefMapper;
import com.harry.aifrontier.mapper.ContentTagMapper;
import com.harry.aifrontier.mapper.SourceMapper;
import com.harry.aifrontier.mapper.TagMapper;
import com.harry.aifrontier.service.ContentService;
import com.harry.aifrontier.util.SlugUtil;
import com.harry.aifrontier.vo.CategoryVO;
import com.harry.aifrontier.vo.ContentAdminListItemVO;
import com.harry.aifrontier.vo.ContentDetailVO;
import com.harry.aifrontier.vo.ContentExternalRefVO;
import com.harry.aifrontier.vo.ContentOptionsVO;
import com.harry.aifrontier.vo.HomeOverviewVO;
import com.harry.aifrontier.vo.OptionVO;
import com.harry.aifrontier.vo.SourceVO;
import com.harry.aifrontier.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final DateTimeFormatter IMPORT_SLUG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int SUMMARY_MAX_LENGTH = 500;
    private static final int AUTHOR_NAME_MAX_LENGTH = 100;

    private final ContentMapper contentMapper;
    private final CategoryMapper categoryMapper;
    private final SourceMapper sourceMapper;
    private final TagMapper tagMapper;
    private final ContentTagMapper contentTagMapper;
    private final ContentExternalRefMapper contentExternalRefMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<ContentAdminListItemVO> adminPage(ContentQueryRequest request) {
        Page<Content> page = contentMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                buildAdminQuery(request)
        );
        return buildPageResult(page, false);
    }

    @Override
    public ContentDetailVO adminDetail(Long id) {
        return toDetailVO(requireContent(id), false);
    }

    @Override
    @Transactional
    public ContentDetailVO create(ContentSaveRequest request) {
        validateAssociations(request);
        Content content = new Content();
        fillContent(content, request);
        content.setViewCount(0);
        contentMapper.insert(content);
        refreshTags(content.getId(), request.getTagIds());
        return toDetailVO(requireContent(content.getId()), false);
    }

    @Override
    @Transactional
    public ContentDetailVO update(Long id, ContentSaveRequest request) {
        validateAssociations(request);
        Content content = requireContent(id);
        fillContent(content, request);
        contentMapper.updateById(content);
        refreshTags(id, request.getTagIds());
        return toDetailVO(requireContent(id), false);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireContent(id);
        contentTagMapper.delete(new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, id));
        contentExternalRefMapper.delete(new LambdaQueryWrapper<ContentExternalRef>().eq(ContentExternalRef::getContentId, id));
        contentMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ContentDetailVO updateStatus(Long id, String publishStatus) {
        Content content = requireContent(id);
        content.setPublishStatus(publishStatus);
        if ("PUBLISHED".equalsIgnoreCase(publishStatus) && content.getPublishedAt() == null) {
            content.setPublishedAt(LocalDateTime.now());
        }
        contentMapper.updateById(content);
        return toDetailVO(requireContent(id), false);
    }

    @Override
    @Transactional
    public ContentExternalRefVO createExternalRef(Long contentId, ContentExternalRefSaveRequest request) {
        requireContent(contentId);
        ContentExternalRef ref = new ContentExternalRef();
        fillExternalRef(ref, contentId, request);
        contentExternalRefMapper.insert(ref);
        return toExternalRefVO(ref);
    }

    @Override
    @Transactional
    public ContentDetailVO importGitHubRepo(GitHubRepoImportRequest request) {
        ContentSaveRequest contentRequest = buildGitHubContentRequest(request);
        ContentDetailVO content = create(contentRequest);

        ContentExternalRefSaveRequest refRequest = new ContentExternalRefSaveRequest();
        refRequest.setRefType("github_repo");
        refRequest.setExternalId(request.getRepoFullName().trim());
        refRequest.setExternalUrl(request.getRepoUrl().trim());
        refRequest.setRawPayloadJson(contentRequest.getExtraJson());
        refRequest.setSyncedAt(LocalDateTime.now());
        createExternalRef(content.getId(), refRequest);

        return adminDetail(content.getId());
    }

    @Override
    @Transactional
    public ContentDetailVO importAiSource(AiSourceImportRequest request) {
        ContentSaveRequest contentRequest = buildAiSourceContentRequest(request);
        ContentDetailVO content = create(contentRequest);

        ContentExternalRefSaveRequest refRequest = new ContentExternalRefSaveRequest();
        refRequest.setRefType(resolveAiSourceRefType(request.getSourceType()));
        refRequest.setExternalId(blankToNull(request.getSourceTitle()));
        refRequest.setExternalUrl(request.getSourceUrl().trim());
        refRequest.setRawPayloadJson(contentRequest.getExtraJson());
        refRequest.setSyncedAt(LocalDateTime.now());
        createExternalRef(content.getId(), refRequest);

        return adminDetail(content.getId());
    }

    @Override
    @Transactional
    public ContentDetailVO importArxivPaper(ArxivPaperImportRequest request) {
        ContentSaveRequest contentRequest = buildArxivContentRequest(request);
        ContentDetailVO content = create(contentRequest);

        ContentExternalRefSaveRequest refRequest = new ContentExternalRefSaveRequest();
        refRequest.setRefType("arxiv_paper");
        refRequest.setExternalId(request.getArxivId().trim());
        refRequest.setExternalUrl("https://arxiv.org/abs/" + request.getArxivId().trim());
        refRequest.setRawPayloadJson(contentRequest.getExtraJson());
        refRequest.setSyncedAt(LocalDateTime.now());
        createExternalRef(content.getId(), refRequest);

        return adminDetail(content.getId());
    }

    @Override
    @Transactional
    public ContentDetailVO importHuggingFacePaper(HuggingFacePaperImportRequest request) {
        ContentSaveRequest contentRequest = buildHuggingFaceContentRequest(request);
        ContentDetailVO content = create(contentRequest);

        ContentExternalRefSaveRequest refRequest = new ContentExternalRefSaveRequest();
        refRequest.setRefType("huggingface_paper");
        refRequest.setExternalId(request.getPaperId().trim());
        refRequest.setExternalUrl(request.getHtmlUrl() != null ? request.getHtmlUrl() : "https://huggingface.co/papers/" + request.getPaperId().trim());
        refRequest.setRawPayloadJson(contentRequest.getExtraJson());
        refRequest.setSyncedAt(LocalDateTime.now());
        createExternalRef(content.getId(), refRequest);

        return adminDetail(content.getId());
    }

    @Override
    @Transactional
    public ContentExternalRefVO updateExternalRef(Long contentId, Long refId, ContentExternalRefSaveRequest request) {
        ContentExternalRef ref = requireExternalRef(contentId, refId);
        fillExternalRef(ref, contentId, request);
        contentExternalRefMapper.updateById(ref);
        return toExternalRefVO(requireExternalRef(contentId, refId));
    }

    @Override
    @Transactional
    public void deleteExternalRef(Long contentId, Long refId) {
        requireExternalRef(contentId, refId);
        contentExternalRefMapper.deleteById(refId);
    }

    @Override
    public ContentOptionsVO options() {
        ContentOptionsVO vo = new ContentOptionsVO();
        vo.setCategories(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getId))
                .stream()
                .map(this::toCategoryVO)
                .toList());
        vo.setTags(tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getId))
                .stream()
                .map(this::toTagVO)
                .toList());
        vo.setSources(sourceMapper.selectList(new LambdaQueryWrapper<Source>().orderByDesc(Source::getId))
                .stream()
                .map(this::toSourceVO)
                .toList());
        vo.setContentTypes(List.of(
                new OptionVO("资讯", "news"),
                new OptionVO("论文", "paper"),
                new OptionVO("项目", "project"),
                new OptionVO("公司动态", "company_update"),
                new OptionVO("技术实践", "practice")
        ));
        vo.setPublishStatuses(List.of(
                new OptionVO("草稿", "DRAFT"),
                new OptionVO("已发布", "PUBLISHED"),
                new OptionVO("已归档", "ARCHIVED")
        ));
        return vo;
    }

    @Override
    public PageResult<ContentAdminListItemVO> portalPage(PortalContentQueryRequest request) {
        List<Long> taggedContentIds = findContentIdsByTag(request.getTagId());
        if (request.getTagId() != null && taggedContentIds.isEmpty()) {
            return PageResult.empty(request.getPageNum(), request.getPageSize());
        }
        Page<Content> page = contentMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                buildPortalQuery(request, taggedContentIds)
        );
        return buildPageResult(page, true);
    }

    @Override
    @Transactional
    public ContentDetailVO portalDetail(Long id) {
        Content content = requireContent(id);
        if (!"PUBLISHED".equals(content.getPublishStatus())) {
            throw new IllegalArgumentException("该内容尚未公开");
        }
        contentMapper.update(null, new LambdaUpdateWrapper<Content>()
                .eq(Content::getId, id)
                .setSql("view_count = view_count + 1"));
        return toDetailVO(requireContent(id), true);
    }

    @Override
    public HomeOverviewVO portalHome() {
        HomeOverviewVO vo = new HomeOverviewVO();
        vo.setFeaturedContents(contentMapper.selectList(new LambdaQueryWrapper<Content>()
                        .eq(Content::getPublishStatus, "PUBLISHED")
                        .gt(Content::getFeaturedLevel, 0)
                        .orderByDesc(Content::getFeaturedLevel)
                        .orderByDesc(Content::getPublishedAt)
                        .last("limit 6"))
                .stream()
                .map(this::toListItemVO)
                .toList());
        vo.setLatestContents(contentMapper.selectList(new LambdaQueryWrapper<Content>()
                        .eq(Content::getPublishStatus, "PUBLISHED")
                        .orderByDesc(Content::getPublishedAt)
                        .orderByDesc(Content::getId)
                        .last("limit 8"))
                .stream()
                .map(this::toListItemVO)
                .toList());
        vo.setCategories(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getIsEnabled, 1)
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getId))
                .stream()
                .map(this::toCategoryVO)
                .toList());
        vo.setSources(sourceMapper.selectList(new LambdaQueryWrapper<Source>()
                        .eq(Source::getIsEnabled, 1)
                        .orderByDesc(Source::getId))
                .stream()
                .map(this::toSourceVO)
                .toList());
        vo.setTags(tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getId))
                .stream()
                .map(this::toTagVO)
                .toList());
        return vo;
    }

    private LambdaQueryWrapper<Content> buildAdminQuery(ContentQueryRequest request) {
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        return new LambdaQueryWrapper<Content>()
                .like(keyword != null && !keyword.isBlank(), Content::getTitle, keyword)
                .eq(request.getContentType() != null && !request.getContentType().isBlank(), Content::getContentType, request.getContentType())
                .eq(request.getCategoryId() != null, Content::getCategoryId, request.getCategoryId())
                .eq(request.getSourceId() != null, Content::getSourceId, request.getSourceId())
                .eq(request.getPublishStatus() != null && !request.getPublishStatus().isBlank(), Content::getPublishStatus, request.getPublishStatus())
                .orderByDesc(Content::getFeaturedLevel)
                .orderByDesc(Content::getPublishedAt)
                .orderByDesc(Content::getId);
    }

    private LambdaQueryWrapper<Content> buildPortalQuery(PortalContentQueryRequest request, List<Long> taggedContentIds) {
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        LambdaQueryWrapper<Content> query = new LambdaQueryWrapper<Content>()
                .eq(Content::getPublishStatus, "PUBLISHED")
                .like(keyword != null && !keyword.isBlank(), Content::getTitle, keyword)
                .eq(request.getContentType() != null && !request.getContentType().isBlank(), Content::getContentType, request.getContentType())
                .eq(request.getCategoryId() != null, Content::getCategoryId, request.getCategoryId())
                .eq(request.getSourceId() != null, Content::getSourceId, request.getSourceId());
        if (request.getTagId() != null) {
            query.in(Content::getId, taggedContentIds);
        }
        applyPortalSort(query, request.getSortBy());
        return query;
    }

    private void applyPortalSort(LambdaQueryWrapper<Content> query, String sortBy) {
        if ("popular".equals(sortBy)) {
            query.orderByDesc(Content::getViewCount)
                    .orderByDesc(Content::getPublishedAt)
                    .orderByDesc(Content::getId);
            return;
        }
        if ("featured".equals(sortBy)) {
            query.orderByDesc(Content::getFeaturedLevel)
                    .orderByDesc(Content::getPublishedAt)
                    .orderByDesc(Content::getId);
            return;
        }
        query.orderByDesc(Content::getPublishedAt)
                .orderByDesc(Content::getId);
    }

    private PageResult<ContentAdminListItemVO> buildPageResult(Page<Content> page, boolean publishedOnly) {
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }
        List<ContentAdminListItemVO> records = page.getRecords().stream()
                .filter(content -> !publishedOnly || "PUBLISHED".equals(content.getPublishStatus()))
                .map(this::toListItemVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    private ContentAdminListItemVO toListItemVO(Content content) {
        Map<Long, Category> categoryMap = findCategoryMap(List.of(content.getCategoryId()));
        Map<Long, Source> sourceMap = findSourceMap(content.getSourceId() == null ? Collections.emptyList() : List.of(content.getSourceId()));
        Map<Long, List<TagVO>> tagMap = findTagMap(List.of(content.getId()));
        ContentAdminListItemVO vo = new ContentAdminListItemVO();
        BeanUtils.copyProperties(content, vo);
        Category category = categoryMap.get(content.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        Source source = sourceMap.get(content.getSourceId());
        if (source != null) {
            vo.setSourceName(source.getName());
            vo.setSourceType(source.getSourceType());
        }
        vo.setTags(tagMap.getOrDefault(content.getId(), Collections.emptyList()));
        return vo;
    }

    private ContentDetailVO toDetailVO(Content content, boolean portalMode) {
        if (portalMode && !"PUBLISHED".equals(content.getPublishStatus())) {
            throw new IllegalArgumentException("该内容尚未公开");
        }
        ContentDetailVO vo = new ContentDetailVO();
        BeanUtils.copyProperties(content, vo);
        Category category = categoryMapper.selectById(content.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        if (content.getSourceId() != null) {
            Source source = sourceMapper.selectById(content.getSourceId());
            if (source != null) {
                vo.setSourceName(source.getName());
                vo.setSourceType(source.getSourceType());
            }
        }
        vo.setTags(findTagMap(List.of(content.getId())).getOrDefault(content.getId(), Collections.emptyList()));
        vo.setExternalRefs(findExternalRefs(content.getId()));
        return vo;
    }

    private void fillContent(Content content, ContentSaveRequest request) {
        content.setTitle(request.getTitle().trim());
        content.setSlug(SlugUtil.resolveSlug(request.getSlug(), request.getTitle(), "content"));
        content.setContentType(request.getContentType());
        content.setSummary(limitText(request.getSummary(), SUMMARY_MAX_LENGTH));
        content.setCoverImage(request.getCoverImage());
        content.setCategoryId(request.getCategoryId());
        content.setSourceId(request.getSourceId());
        content.setSourceUrl(request.getSourceUrl());
        content.setAuthorName(limitText(request.getAuthorName(), AUTHOR_NAME_MAX_LENGTH));
        content.setPublishStatus(request.getPublishStatus());
        content.setFeaturedLevel(request.getFeaturedLevel());
        content.setReadingTime(request.getReadingTime());
        content.setPublishedAt(resolvePublishedAt(request));
        content.setBodyMarkdown(request.getBodyMarkdown());
        content.setExtraJson(request.getExtraJson());
    }

    private LocalDateTime resolvePublishedAt(ContentSaveRequest request) {
        if ("PUBLISHED".equals(request.getPublishStatus()) && request.getPublishedAt() == null) {
            return LocalDateTime.now();
        }
        return request.getPublishedAt();
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private void validateAssociations(ContentSaveRequest request) {
        if (categoryMapper.selectById(request.getCategoryId()) == null) {
            throw new IllegalArgumentException("所选分类不存在");
        }
        if (request.getSourceId() != null && sourceMapper.selectById(request.getSourceId()) == null) {
            throw new IllegalArgumentException("所选来源不存在");
        }
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            long tagCount = tagMapper.selectCount(new LambdaQueryWrapper<Tag>().in(Tag::getId, request.getTagIds()));
            if (tagCount != request.getTagIds().size()) {
                throw new IllegalArgumentException("存在无效标签，请重新选择");
            }
        }
    }

    private void refreshTags(Long contentId, List<Long> tagIds) {
        contentTagMapper.delete(new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, contentId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds.stream().filter(Objects::nonNull).distinct().toList()) {
            ContentTag relation = new ContentTag();
            relation.setContentId(contentId);
            relation.setTagId(tagId);
            contentTagMapper.insert(relation);
        }
    }

    private Content requireContent(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new IllegalArgumentException("内容不存在");
        }
        return content;
    }

    private ContentExternalRef requireExternalRef(Long contentId, Long refId) {
        ContentExternalRef ref = contentExternalRefMapper.selectById(refId);
        if (ref == null || !Objects.equals(ref.getContentId(), contentId)) {
            throw new IllegalArgumentException("外部引用记录不存在");
        }
        return ref;
    }

    private void fillExternalRef(ContentExternalRef ref, Long contentId, ContentExternalRefSaveRequest request) {
        ref.setContentId(contentId);
        ref.setRefType(request.getRefType().trim());
        ref.setExternalId(blankToNull(request.getExternalId()));
        ref.setExternalUrl(blankToNull(request.getExternalUrl()));
        ref.setRawPayloadJson(blankToNull(request.getRawPayloadJson()));
        ref.setSyncedAt(request.getSyncedAt());
    }

    private ContentSaveRequest buildGitHubContentRequest(GitHubRepoImportRequest request) {
        ContentSaveRequest contentRequest = new ContentSaveRequest();
        String repoFullName = request.getRepoFullName().trim();
        String repoUrl = request.getRepoUrl().trim();
        String language = blankToNull(request.getLanguage());
        List<String> topics = parseTopics(request.getTopics());
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("externalType", "github_repo");
        extra.put("repoFullName", repoFullName);
        extra.put("repoUrl", repoUrl);
        extra.put("stars", request.getStars() == null ? 0 : request.getStars());
        extra.put("language", language);
        extra.put("topics", topics);
        extra.put("homepage", blankToNull(request.getHomepage()));
        extra.put("importMode", "manual_demo");

        contentRequest.setTitle(repoFullName + "：GitHub AI 项目观察");
        contentRequest.setSlug("github-" + repoFullName.replaceAll("[^A-Za-z0-9]+", "-") + "-" + LocalDateTime.now().format(IMPORT_SLUG_TIME_FORMATTER));
        contentRequest.setContentType("project");
        contentRequest.setSummary(request.getDescription().trim());
        contentRequest.setCategoryId(request.getCategoryId());
        contentRequest.setSourceId(request.getSourceId());
        contentRequest.setSourceUrl(repoUrl);
        contentRequest.setAuthorName("GitHub 手动导入");
        contentRequest.setPublishStatus(request.getPublishStatus());
        contentRequest.setFeaturedLevel(1);
        contentRequest.setReadingTime(6);
        contentRequest.setBodyMarkdown(buildGitHubBodyMarkdown(request, topics));
        contentRequest.setExtraJson(writeJson(extra));
        contentRequest.setTagIds(request.getTagIds());
        return contentRequest;
    }

    private ContentSaveRequest buildArxivContentRequest(ArxivPaperImportRequest request) {
        ContentSaveRequest contentRequest = new ContentSaveRequest();
        contentRequest.setTitle(request.getTitle().trim());
        contentRequest.setSlug("arxiv-" + request.getArxivId().trim().replace(".", "-"));
        contentRequest.setContentType("paper");
        contentRequest.setSummary(request.getAbstractText().trim());
        contentRequest.setCoverImage(null);
        contentRequest.setCategoryId(request.getCategoryId());
        contentRequest.setSourceId(request.getSourceId());
        contentRequest.setSourceUrl("https://arxiv.org/abs/" + request.getArxivId().trim());
        contentRequest.setAuthorName(request.getAuthors() != null ? String.join(", ", request.getAuthors()) : "arXiv");
        contentRequest.setPublishStatus(request.getPublishStatus());
        contentRequest.setFeaturedLevel(1);
        contentRequest.setReadingTime(10);
        contentRequest.setBodyMarkdown(resolveArxivBodyMarkdown(request));
        contentRequest.setExtraJson(buildArxivExtraJson(request));
        contentRequest.setTagIds(request.getTagIds());
        return contentRequest;
    }

    private String resolveArxivBodyMarkdown(ArxivPaperImportRequest request) {
        if (request.getBodyMarkdown() != null && !request.getBodyMarkdown().isBlank()) {
            return request.getBodyMarkdown().trim();
        }
        String authors = request.getAuthors() != null ? String.join(", ", request.getAuthors()) : "未知作者";
        return """
                ## 论文摘要

                %s

                ## 作者

                %s

                ## 相关链接

                - [arXiv 页面](https://arxiv.org/abs/%s)
                - [PDF 下载](https://arxiv.org/pdf/%s)
                """.formatted(
                request.getAbstractText().trim(),
                authors,
                request.getArxivId().trim(),
                request.getArxivId().trim()
        ).trim();
    }

    private String buildArxivExtraJson(ArxivPaperImportRequest request) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("externalType", "arxiv_paper");
        extra.put("arxivId", request.getArxivId().trim());
        extra.put("authors", request.getAuthors() != null ? request.getAuthors() : Collections.emptyList());
        extra.put("pdfUrl", request.getPdfUrl() != null ? request.getPdfUrl() : "https://arxiv.org/pdf/" + request.getArxivId().trim());
        extra.put("importMode", "arxiv_direct_import");
        return writeJson(extra);
    }

    private ContentSaveRequest buildHuggingFaceContentRequest(HuggingFacePaperImportRequest request) {
        ContentSaveRequest contentRequest = new ContentSaveRequest();
        contentRequest.setTitle(request.getTitle().trim());
        contentRequest.setSlug("hf-" + request.getPaperId().trim().replace(".", "-"));
        contentRequest.setContentType("paper");
        contentRequest.setSummary(request.getAbstractText().trim());
        contentRequest.setCoverImage(null);
        contentRequest.setCategoryId(request.getCategoryId());
        contentRequest.setSourceId(request.getSourceId());
        contentRequest.setSourceUrl(request.getHtmlUrl() != null ? request.getHtmlUrl() : "https://huggingface.co/papers/" + request.getPaperId().trim());
        contentRequest.setAuthorName(request.getAuthors() != null ? String.join(", ", request.getAuthors()) : "HuggingFace");
        contentRequest.setPublishStatus(request.getPublishStatus());
        contentRequest.setFeaturedLevel(request.getLikes() != null && request.getLikes() > 100 ? 3 : request.getLikes() != null && request.getLikes() > 50 ? 2 : 1);
        contentRequest.setReadingTime(10);
        contentRequest.setBodyMarkdown(resolveHuggingFaceBodyMarkdown(request));
        contentRequest.setExtraJson(buildHuggingFaceExtraJson(request));
        contentRequest.setTagIds(request.getTagIds());
        return contentRequest;
    }

    private String resolveHuggingFaceBodyMarkdown(HuggingFacePaperImportRequest request) {
        if (request.getBodyMarkdown() != null && !request.getBodyMarkdown().isBlank()) {
            return request.getBodyMarkdown().trim();
        }
        String authors = request.getAuthors() != null ? String.join(", ", request.getAuthors()) : "未知作者";
        String stats = String.format("点赞数：%s | 评论数：%s",
                request.getLikes() != null ? request.getLikes() : "未知",
                request.getComments() != null ? request.getComments() : "未知");
        return """
                ## 论文摘要

                %s

                ## 作者

                %s

                ## 社区热度

                %s

                ## 相关链接

                - [HuggingFace 页面](%s)
                """.formatted(
                request.getAbstractText().trim(),
                authors,
                stats,
                request.getHtmlUrl() != null ? request.getHtmlUrl() : "https://huggingface.co/papers/" + request.getPaperId().trim()
        ).trim();
    }

    private String buildHuggingFaceExtraJson(HuggingFacePaperImportRequest request) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("externalType", "huggingface_paper");
        extra.put("huggingfaceId", request.getPaperId().trim());
        extra.put("authors", request.getAuthors() != null ? request.getAuthors() : Collections.emptyList());
        extra.put("likes", request.getLikes());
        extra.put("comments", request.getComments());
        extra.put("importMode", "huggingface_daily_papers");
        return writeJson(extra);
    }

    private String buildGitHubBodyMarkdown(GitHubRepoImportRequest request, List<String> topics) {
        String repoFullName = request.getRepoFullName().trim();
        String language = blankToNull(request.getLanguage());
        String homepage = blankToNull(request.getHomepage());
        return """
                ## 项目概览

                %s

                ## GitHub 信号

                - 仓库：[%s](%s)
                - Star 数：%s
                - 主要语言：%s
                - Topics：%s

                ## 为什么值得关注

                这条内容来自后台的 GitHub 项目手动导入 Demo。当前阶段不直接调用 GitHub API，而是先把仓库信息转成平台内部统一内容，并写入外部引用关系。后续如果接入真实采集任务，可以复用这套内容结构和 `github_repo` 外部映射。

                %s
                """.formatted(
                request.getDescription().trim(),
                repoFullName,
                request.getRepoUrl().trim(),
                request.getStars() == null ? 0 : request.getStars(),
                language == null ? "未填写" : language,
                topics.isEmpty() ? "未填写" : String.join(", ", topics),
                homepage == null ? "" : "项目主页：" + homepage
        ).trim();
    }

    private ContentSaveRequest buildAiSourceContentRequest(AiSourceImportRequest request) {
        ContentSaveRequest contentRequest = new ContentSaveRequest();
        String title = request.getTitle().trim();
        contentRequest.setTitle(title);
        contentRequest.setSlug("ai-source-" + LocalDateTime.now().format(IMPORT_SLUG_TIME_FORMATTER));
        contentRequest.setContentType(request.getContentType());
        contentRequest.setSummary(request.getSummary().trim());
        contentRequest.setCoverImage(blankToNull(request.getCoverImage()));
        contentRequest.setCategoryId(request.getCategoryId());
        contentRequest.setSourceId(request.getSourceId());
        contentRequest.setSourceUrl(request.getSourceUrl().trim());
        contentRequest.setAuthorName("百炼辅助整理");
        contentRequest.setPublishStatus("DRAFT");
        contentRequest.setFeaturedLevel(resolveImportanceAsFeatured(request.getImportanceScore()));
        contentRequest.setReadingTime(6);
        contentRequest.setBodyMarkdown(resolveAiSourceBodyMarkdown(request));
        contentRequest.setExtraJson(buildAiSourceExtraJson(request));
        contentRequest.setTagIds(request.getTagIds());
        return contentRequest;
    }

    private String resolveAiSourceBodyMarkdown(AiSourceImportRequest request) {
        if (request.getBodyMarkdown() != null && !request.getBodyMarkdown().isBlank()) {
            return request.getBodyMarkdown().trim();
        }
        return """
                ## AI 导读

                %s

                ## 来源说明

                %s

                ## 原文摘要/摘录

                %s

                ## 为什么值得关注

                %s

                ## 原始链接

                [%s](%s)
                """.formatted(
                valueOrDefault(request.getAiSummary(), request.getSummary()),
                valueOrDefault(request.getSourceBrief(), "该来源由后台人工录入，并由百炼生成结构化导读。"),
                valueOrDefault(request.getOriginalExcerpt(), request.getOriginalSummary()),
                valueOrDefault(request.getRecommendationReason(), "根据当前录入信息判断，该内容适合作为 AI 前沿动态收录。"),
                valueOrDefault(request.getSourceTitle(), request.getSourceUrl()),
                request.getSourceUrl()
        ).trim();
    }

    private String buildAiSourceExtraJson(AiSourceImportRequest request) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("externalType", resolveAiSourceRefType(request.getSourceType()));
        extra.put("aiSummary", blankToNull(request.getAiSummary()));
        extra.put("recommendationReason", blankToNull(request.getRecommendationReason()));
        extra.put("importanceScore", request.getImportanceScore());
        extra.put("tagSuggestions", request.getTagSuggestions() == null ? Collections.emptyList() : request.getTagSuggestions());
        extra.put("sourceBrief", blankToNull(request.getSourceBrief()));
        extra.put("sourceTitle", blankToNull(request.getSourceTitle()));
        extra.put("sourceName", blankToNull(request.getSourceName()));
        extra.put("sourceType", blankToNull(request.getSourceType()));
        extra.put("sourceImage", blankToNull(request.getCoverImage()));
        extra.put("originalSummary", blankToNull(request.getOriginalSummary()));
        extra.put("originalExcerpt", blankToNull(request.getOriginalExcerpt()));
        extra.put("aiProvider", "Alibaba Bailian / DashScope");
        extra.put("importMode", "manual_source_ai_summary");
        return writeJson(extra);
    }

    private String resolveAiSourceRefType(String sourceType) {
        if ("github".equals(sourceType)) {
            return "github_repo";
        }
        if ("paper".equals(sourceType)) {
            return "arxiv_paper";
        }
        if ("official_blog".equals(sourceType)) {
            return "official_post";
        }
        if ("community".equals(sourceType)) {
            return "community_practice";
        }
        if ("leaderboard".equals(sourceType)) {
            return "leaderboard_item";
        }
        return "manual_source";
    }

    private int resolveImportanceAsFeatured(Integer importanceScore) {
        if (importanceScore == null) {
            return 1;
        }
        if (importanceScore >= 85) {
            return 3;
        }
        if (importanceScore >= 70) {
            return 2;
        }
        return 1;
    }

    private List<String> parseTopics(String topics) {
        if (topics == null || topics.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(topics.split("[,，\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("GitHub 项目扩展字段生成失败");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private Map<Long, Category> findCategoryMap(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds.stream().filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Source> findSourceMap(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sourceMapper.selectBatchIds(sourceIds.stream().filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Source::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, List<TagVO>> findTagMap(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ContentTag> relations = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                .in(ContentTag::getContentId, contentIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = relations.stream().map(ContentTag::getTagId).collect(Collectors.toSet());
        Map<Long, TagVO> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                .map(this::toTagVO)
                .collect(Collectors.toMap(TagVO::getId, Function.identity()));
        Map<Long, List<TagVO>> result = new LinkedHashMap<>();
        for (ContentTag relation : relations) {
            TagVO tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(relation.getContentId(), key -> new ArrayList<>()).add(tag);
        }
        return result;
    }

    private List<Long> findContentIdsByTag(Long tagId) {
        if (tagId == null) {
            return Collections.emptyList();
        }
        return contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                        .eq(ContentTag::getTagId, tagId))
                .stream()
                .map(ContentTag::getContentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<ContentExternalRefVO> findExternalRefs(Long contentId) {
        if (contentId == null) {
            return Collections.emptyList();
        }
        return contentExternalRefMapper.selectList(new LambdaQueryWrapper<ContentExternalRef>()
                        .eq(ContentExternalRef::getContentId, contentId)
                        .orderByDesc(ContentExternalRef::getSyncedAt)
                        .orderByDesc(ContentExternalRef::getId))
                .stream()
                .map(this::toExternalRefVO)
                .toList();
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        return vo;
    }

    private SourceVO toSourceVO(Source source) {
        SourceVO vo = new SourceVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private ContentExternalRefVO toExternalRefVO(ContentExternalRef ref) {
        ContentExternalRefVO vo = new ContentExternalRefVO();
        BeanUtils.copyProperties(ref, vo);
        return vo;
    }
}
