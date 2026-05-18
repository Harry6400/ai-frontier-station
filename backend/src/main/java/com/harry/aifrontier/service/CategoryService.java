package com.harry.aifrontier.service;

import com.harry.aifrontier.dto.request.CategorySaveRequest;
import com.harry.aifrontier.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> listAll();

    CategoryVO create(CategorySaveRequest request);

    CategoryVO update(Long id, CategorySaveRequest request);

    void delete(Long id);
}
