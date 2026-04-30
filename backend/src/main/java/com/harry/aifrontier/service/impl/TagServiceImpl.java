package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.TagSaveRequest;
import com.harry.aifrontier.entity.Tag;
import com.harry.aifrontier.mapper.TagMapper;
import com.harry.aifrontier.service.TagService;
import com.harry.aifrontier.util.SlugUtil;
import com.harry.aifrontier.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagVO> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public TagVO create(TagSaveRequest request) {
        Tag tag = new Tag();
        fillTag(tag, request);
        tagMapper.insert(tag);
        return toVO(tagMapper.selectById(tag.getId()));
    }

    @Override
    public TagVO update(Long id, TagSaveRequest request) {
        Tag tag = requireTag(id);
        fillTag(tag, request);
        tagMapper.updateById(tag);
        return toVO(tagMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        requireTag(id);
        tagMapper.deleteById(id);
    }

    private void fillTag(Tag tag, TagSaveRequest request) {
        tag.setName(request.getName().trim());
        tag.setSlug(SlugUtil.resolveSlug(request.getSlug(), request.getName(), "tag"));
        tag.setColor(request.getColor());
        tag.setDescription(request.getDescription());
    }

    private Tag requireTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        return tag;
    }

    private TagVO toVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        return vo;
    }
}
