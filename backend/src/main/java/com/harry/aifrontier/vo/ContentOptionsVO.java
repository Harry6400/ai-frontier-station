package com.harry.aifrontier.vo;

import lombok.Data;

import java.util.List;

@Data
public class ContentOptionsVO {

    private List<CategoryVO> categories;

    private List<TagVO> tags;

    private List<SourceVO> sources;

    private List<OptionVO> contentTypes;

    private List<OptionVO> publishStatuses;
}
