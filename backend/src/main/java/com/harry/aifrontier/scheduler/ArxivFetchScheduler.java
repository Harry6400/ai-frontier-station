package com.harry.aifrontier.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.ArxivPaperImportRequest;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.entity.Content;
import com.harry.aifrontier.entity.ContentExternalRef;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.mapper.ContentExternalRefMapper;
import com.harry.aifrontier.mapper.ContentMapper;
import com.harry.aifrontier.service.ArxivService;
import com.harry.aifrontier.service.ContentService;
import com.harry.aifrontier.service.FetchLogService;
import com.harry.aifrontier.service.PaperSummaryService;
import com.harry.aifrontier.vo.ArxivPaperCandidateVO;
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
public class ArxivFetchScheduler {

    private final ArxivService arxivService;
    private final ContentService contentService;
    private final FetchLogService fetchLogService;
    private final ContentExternalRefMapper contentExternalRefMapper;
    private final CategoryMapper categoryMapper;
    private final ContentMapper contentMapper;
    private final PaperSummaryService paperSummaryService;

    private static final List<String> KEYWORDS = List.of(
            "low-dose CT",
            "CT denoising",
            "medical image denoising",
            "diffusion model for CT",
            "sparse-view CT",
            "3D CT reconstruction",
            "CT artifact reduction",
            "noise reduction medical imaging"
    );

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledFetch() {
        runOnce();
    }

    public void runOnce() {
        log.info("[arXiv 定时采集] 开始执行...");
        Long categoryId = resolveCategoryId();
        Set<String> processedIds = new HashSet<>();
        int totalFound = 0;
        int totalImported = 0;

        for (String keyword : KEYWORDS) {
            try {
                log.info("[arXiv 定时采集] 搜索关键词: {}", keyword);
                PageResult<ArxivPaperCandidateVO> result = arxivService.searchPapers(keyword, 10, 0);
                List<ArxivPaperCandidateVO> papers = result.getRecords();
                int found = papers.size();
                int imported = 0;

                for (ArxivPaperCandidateVO paper : papers) {
                    String arxivId = paper.getArxivId();
                    if (arxivId == null || arxivId.isBlank()) {
                        continue;
                    }
                    arxivId = arxivId.trim();

                    // 跳过本批次已处理的
                    if (processedIds.contains(arxivId)) {
                        continue;
                    }

                    // 检查是否已入库
                    if (isAlreadyImported(arxivId)) {
                        processedIds.add(arxivId);
                        continue;
                    }

                    try {
                        ArxivPaperImportRequest importRequest = buildImportRequest(paper, categoryId);
                        contentService.importArxivPaper(importRequest);
                        imported++;
                        processedIds.add(arxivId);
                        log.info("[arXiv 定时采集] 导入成功: {} - {}", arxivId, paper.getTitle());

                        // 生成中文摘要
                        try {
                            String chineseSummary = paperSummaryService.generateChineseSummary(
                                    paper.getTitle(), paper.getAbstractText());
                            updateContentSummary(arxivId, "arxiv_paper", chineseSummary);
                            log.info("[arXiv 定时采集] 中文摘要生成成功: {}", arxivId);
                            Thread.sleep(2000);
                        } catch (Exception se) {
                            log.warn("[arXiv 定时采集] 中文摘要生成失败（不影响导入）: {} - {}", arxivId, se.getMessage());
                        }
                    } catch (Exception e) {
                        log.warn("[arXiv 定时采集] 导入失败: {} - {}", arxivId, e.getMessage());
                    }
                }

                totalFound += found;
                totalImported += imported;
                fetchLogService.log("arxiv", keyword, found, imported, "SUCCESS", null);
                log.info("[arXiv 定时采集] 关键词 '{}' 完成: found={}, imported={}", keyword, found, imported);

            } catch (Exception e) {
                log.error("[arXiv 定时采集] 关键词 '{}' 失败: {}", keyword, e.getMessage(), e);
                fetchLogService.log("arxiv", keyword, 0, 0, "FAILED", e.getMessage());
            }

            // 关键词之间等待 3 秒，避免 arXiv 限流
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[arXiv 定时采集] 线程被中断，提前结束");
                break;
            }
        }

        log.info("[arXiv 定时采集] 执行完毕: totalFound={}, totalImported={}", totalFound, totalImported);
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
            log.warn("[arXiv 定时采集] 更新中文摘要字段失败: {}", e.getMessage());
        }
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
            log.warn("[arXiv 定时采集] 查询论文分类失败，使用默认 ID=1: {}", e.getMessage());
        }
        return 1L;
    }

    private boolean isAlreadyImported(String arxivId) {
        return contentExternalRefMapper.selectCount(
                new LambdaQueryWrapper<ContentExternalRef>()
                        .eq(ContentExternalRef::getRefType, "arxiv_paper")
                        .eq(ContentExternalRef::getExternalId, arxivId)
        ) > 0;
    }

    private ArxivPaperImportRequest buildImportRequest(ArxivPaperCandidateVO paper, Long categoryId) {
        ArxivPaperImportRequest request = new ArxivPaperImportRequest();
        request.setArxivId(paper.getArxivId().trim());
        request.setTitle(paper.getTitle());
        request.setAuthors(paper.getAuthors());
        request.setAbstractText(paper.getAbstractText());
        request.setPdfUrl(paper.getPdfUrl());
        request.setCategoryId(categoryId);
        request.setSourceId(null);
        request.setPublishStatus("PUBLISHED");
        request.setSummary(null);
        request.setBodyMarkdown(paper.getAbstractText());
        return request;
    }
}
