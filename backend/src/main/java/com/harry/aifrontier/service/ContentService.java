package com.harry.aifrontier.service;

import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.AiSourceImportRequest;
import com.harry.aifrontier.dto.request.ArxivPaperImportRequest;
import com.harry.aifrontier.dto.request.ContentQueryRequest;
import com.harry.aifrontier.dto.request.ContentExternalRefSaveRequest;
import com.harry.aifrontier.dto.request.ContentSaveRequest;
import com.harry.aifrontier.dto.request.GitHubRepoImportRequest;
import com.harry.aifrontier.dto.request.PortalContentQueryRequest;
import com.harry.aifrontier.vo.ContentAdminListItemVO;
import com.harry.aifrontier.vo.ContentDetailVO;
import com.harry.aifrontier.vo.ContentExternalRefVO;
import com.harry.aifrontier.vo.ContentOptionsVO;
import com.harry.aifrontier.vo.HomeOverviewVO;

public interface ContentService {

    PageResult<ContentAdminListItemVO> adminPage(ContentQueryRequest request);

    ContentDetailVO adminDetail(Long id);

    ContentDetailVO create(ContentSaveRequest request);

    ContentDetailVO update(Long id, ContentSaveRequest request);

    void delete(Long id);

    ContentDetailVO updateStatus(Long id, String publishStatus);

    ContentExternalRefVO createExternalRef(Long contentId, ContentExternalRefSaveRequest request);

    ContentDetailVO importGitHubRepo(GitHubRepoImportRequest request);

    ContentDetailVO importAiSource(AiSourceImportRequest request);

    ContentDetailVO importArxivPaper(ArxivPaperImportRequest request);

    ContentExternalRefVO updateExternalRef(Long contentId, Long refId, ContentExternalRefSaveRequest request);

    void deleteExternalRef(Long contentId, Long refId);

    ContentOptionsVO options();

    PageResult<ContentAdminListItemVO> portalPage(PortalContentQueryRequest request);

    ContentDetailVO portalDetail(Long id);

    HomeOverviewVO portalHome();
}
