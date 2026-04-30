package com.harry.aifrontier.service;

import com.harry.aifrontier.dto.request.SourceSaveRequest;
import com.harry.aifrontier.vo.SourceVO;

import java.util.List;

public interface SourceService {

    List<SourceVO> listAll();

    SourceVO create(SourceSaveRequest request);

    SourceVO update(Long id, SourceSaveRequest request);

    void delete(Long id);
}
