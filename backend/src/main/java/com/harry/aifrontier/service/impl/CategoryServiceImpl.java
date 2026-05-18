package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.CategorySaveRequest;
import com.harry.aifrontier.entity.Category;
import com.harry.aifrontier.mapper.CategoryMapper;
import com.harry.aifrontier.service.CategoryService;
import com.harry.aifrontier.util.SlugUtil;
import com.harry.aifrontier.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByDesc(Category::getIsEnabled)
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CategoryVO create(CategorySaveRequest request) {
        Category category = new Category();
        fillCategory(category, request);
        categoryMapper.insert(category);
        return toVO(categoryMapper.selectById(category.getId()));
    }

    @Override
    public CategoryVO update(Long id, CategorySaveRequest request) {
        Category category = requireCategory(id);
        fillCategory(category, request);
        categoryMapper.updateById(category);
        return toVO(categoryMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        requireCategory(id);
        categoryMapper.deleteById(id);
    }

    private void fillCategory(Category category, CategorySaveRequest request) {
        category.setName(request.getName().trim());
        category.setSlug(SlugUtil.resolveSlug(request.getSlug(), request.getName(), "category"));
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder());
        category.setIsEnabled(request.getIsEnabled());
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
