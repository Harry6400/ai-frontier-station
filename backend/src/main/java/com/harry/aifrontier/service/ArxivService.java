package com.harry.aifrontier.service;

import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.vo.ArxivPaperCandidateVO;

public interface ArxivService {

    PageResult<ArxivPaperCandidateVO> searchPapers(String query, int maxResults, int start);

    ArxivPaperCandidateVO findPaper(String arxivId);
}
