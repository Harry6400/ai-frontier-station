package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.SourceSaveRequest;
import com.harry.aifrontier.entity.Source;
import com.harry.aifrontier.mapper.SourceMapper;
import com.harry.aifrontier.service.SourceService;
import com.harry.aifrontier.util.SlugUtil;
import com.harry.aifrontier.vo.SourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {

    private final SourceMapper sourceMapper;

    @Override
    public List<SourceVO> listAll() {
        return sourceMapper.selectList(new LambdaQueryWrapper<Source>().orderByDesc(Source::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public SourceVO create(SourceSaveRequest request) {
        Source source = new Source();
        fillSource(source, request);
        sourceMapper.insert(source);
        return toVO(sourceMapper.selectById(source.getId()));
    }

    @Override
    public SourceVO update(Long id, SourceSaveRequest request) {
        Source source = requireSource(id);
        fillSource(source, request);
        sourceMapper.updateById(source);
        return toVO(sourceMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        requireSource(id);
        sourceMapper.deleteById(id);
    }

    private void fillSource(Source source, SourceSaveRequest request) {
        source.setName(request.getName().trim());
        source.setSlug(SlugUtil.resolveSlug(request.getSlug(), request.getName(), "source"));
        source.setSourceType(request.getSourceType());
        source.setWebsiteUrl(request.getWebsiteUrl());
        source.setDescription(request.getDescription());
        source.setIsEnabled(request.getIsEnabled());
    }

    private Source requireSource(Long id) {
        Source source = sourceMapper.selectById(id);
        if (source == null) {
            throw new IllegalArgumentException("来源不存在");
        }
        return source;
    }

    private SourceVO toVO(Source source) {
        SourceVO vo = new SourceVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}
