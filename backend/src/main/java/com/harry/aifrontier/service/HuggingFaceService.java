package com.harry.aifrontier.service;

import com.harry.aifrontier.vo.HuggingFacePaperCandidateVO;

import java.util.List;

public interface HuggingFaceService {

    List<HuggingFacePaperCandidateVO> getDailyPapers();

    HuggingFacePaperCandidateVO findPaper(String paperId);
}
