package com.harry.aifrontier.service;

import com.harry.aifrontier.dto.request.TagSaveRequest;
import com.harry.aifrontier.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> listAll();

    TagVO create(TagSaveRequest request);

    TagVO update(Long id, TagSaveRequest request);

    void delete(Long id);
}
