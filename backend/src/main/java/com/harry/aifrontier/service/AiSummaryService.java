package com.harry.aifrontier.service;

import com.harry.aifrontier.dto.request.AiSourceSummaryRequest;
import com.harry.aifrontier.vo.AiSourceSummaryVO;

public interface AiSummaryService {

    AiSourceSummaryVO summarizeSource(AiSourceSummaryRequest request);
}
