package com.harry.aifrontier.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.HuggingFacePaperImportRequest;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentExternalRef;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentExternalRefMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.service.ContentService;
import com.harry.aifrontier.service.FetchLogService;
import com.harry.aifrontier.service.HuggingFaceService;
import com.harry.aifrontier.service.PaperSummaryService;
import com.harry.aifrontier.vo.HuggingFacePaperCandidateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HuggingFaceFetchScheduler {

    private final HuggingFaceService huggingFaceService;
    private final ContentService contentService;
    private final FetchLogService fetchLogService;
    private final ContentExternalRefMapper contentExternalRefMapper;
    private final CategoryMapper categoryMapper;
    private final ContentMapper contentMapper;
    private final PaperSummaryService paperSummaryService;

    private static final List<String> KEYWORDS = List.of(
            "medical", "imaging", "ct", "denoising", "segmentation",
            "reconstruction", "diffusion", "radiology", "pathology",
            "volumetric", "3d", "x-ray", "noise"
    );

    /**
     * 每天凌晨4点执行（错开 arXiv 的3点）
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void scheduledFetch() {
        runOnce();
    }

    /**
     * 手动触发一次 HuggingFace Daily Papers 采集
     */
    public void runOnce() {
        log.info("========== HuggingFace Daily Papers 定时采集开始 ==========");
        int found = 0;
        int imported = 0;
        String status = "SUCCESS";
        String message = "";

        try {
            // 1. 获取全部 Daily Papers
            List<HuggingFacePaperCandidateVO> allPapers = huggingFaceService.getDailyPapers();
            log.info("HuggingFace API 返回论文总数: {}", allPapers.size());

            // 2. 过滤关键词
            List<HuggingFacePaperCandidateVO> filtered = allPapers.stream()
                    .filter(this::matchesKeywords)
                    .toList();
            found = filtered.size();
            log.info("关键词过滤后论文数: {}", found);

            // 3. 查找"论文"分类
            Long categoryId = resolveCategoryId();

            // 4. 去重并导入
            Set<String> processedIds = new HashSet<>();
            for (HuggingFacePaperCandidateVO paper : filtered) {
                String paperId = paper.getPaperId();
                if (paperId == null || paperId.isBlank()) {
                    continue;
                }

                // 批次内去重
                if (!processedIds.add(paperId)) {
                    log.debug("批次内重复，跳过: {}", paperId);
                    continue;
                }

                // 数据库去重
                if (alreadyImported(paperId)) {
                    log.debug("已导入过，跳过: {}", paperId);
                    continue;
                }

                try {
                    HuggingFacePaperImportRequest request = buildImportRequest(paper, categoryId);
                    contentService.importHuggingFacePaper(request);
                    imported++;
                    log.info("成功导入 HuggingFace 论文: {} - {}", paperId, paper.getTitle());

                    // 生成中文摘要
                    try {
                        String chineseSummary = paperSummaryService.generateChineseSummary(
                                paper.getTitle(), paper.getAbstractText());
                        updateContentSummary(paperId, "huggingface_paper", chineseSummary);
                        log.info("HuggingFace 中文摘要生成成功: {}", paperId);
                    } catch (Exception se) {
                        log.warn("HuggingFace 中文摘要生成失败（不影响导入）: {} - {}", paperId, se.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("导入 HuggingFace 论文失败 [{}]: {}", paperId, e.getMessage());
                }
            }

            message = String.format("获取 %d 篇，过滤 %d 篇，成功导入 %d 篇", allPapers.size(), found, imported);
            log.info("HuggingFace Daily Papers 采集完成: {}", message);

        } catch (Exception e) {
            status = "FAILED";
            message = "HuggingFace 采集异常: " + e.getMessage();
            log.error("HuggingFace Daily Papers 采集失败: {}", e.getMessage(), e);
        } finally {
            fetchLogService.log("huggingface", null, found, imported, status, message);
            log.info("========== HuggingFace Daily Papers 定时采集结束 ==========");
        }
    }

    private void updateContentSummary(String externalId, String refType, String chineseSummary) {
        try {
            ContentExternalRef ref = contentExternalRefMapper.selectOne(
                    new LambdaQueryWrapper<ContentExternalRef>()
                            .eq(ContentExternalRef::getRefType, refType)
                            .eq(ContentExternalRef::getExternalId, externalId)
                            .last("LIMIT 1")
            );
            if (ref != null && ref.getContentId() != null) {
                Content content = new Content();
                content.setId(ref.getContentId());
                content.setSummary(chineseSummary);
                contentMapper.updateById(content);
            }
        } catch (Exception e) {
            log.warn("更新中文摘要字段失败: {}", e.getMessage());
        }
    }

    private boolean matchesKeywords(HuggingFacePaperCandidateVO paper) {
        String title = paper.getTitle() != null ? paper.getTitle().toLowerCase() : "";
        String abstractText = paper.getAbstractText() != null ? paper.getAbstractText().toLowerCase() : "";
        String combined = title + " " + abstractText;
        return KEYWORDS.stream().anyMatch(combined::contains);
    }

    private boolean alreadyImported(String paperId) {
        return contentExternalRefMapper.selectCount(
                new LambdaQueryWrapper<ContentExternalRef>()
                        .eq(ContentExternalRef::getRefType, "huggingface_paper")
                        .eq(ContentExternalRef::getExternalId, paperId)
        ) > 0;
    }

    private Long resolveCategoryId() {
        try {
            Category category = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>()
                            .like(Category::getName, "论文")
                            .last("LIMIT 1")
            );
            if (category != null) {
                return category.getId();
            }
        } catch (Exception e) {
            log.warn("查询论文分类失败，使用默认 ID=1: {}", e.getMessage());
        }
        return 1L;
    }

    private HuggingFacePaperImportRequest buildImportRequest(HuggingFacePaperCandidateVO paper, Long categoryId) {
        HuggingFacePaperImportRequest request = new HuggingFacePaperImportRequest();
        request.setPaperId(paper.getPaperId());
        request.setTitle(paper.getTitle());
        request.setAbstractText(paper.getAbstractText());
        request.setAuthors(paper.getAuthors());
        request.setHtmlUrl(paper.getHtmlUrl());
        request.setLikes(paper.getLikes());
        request.setComments(paper.getComments());
        request.setCategoryId(categoryId);
        request.setPublishStatus("PUBLISHED");
        return request;
    }
}
