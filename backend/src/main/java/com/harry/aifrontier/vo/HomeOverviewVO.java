package com.harry.aifrontier.vo;

import lombok.Data;

import java.util.List;

@Data
public class HomeOverviewVO {

    private List<ContentAdminListItemVO> featuredContents;

    private List<ContentAdminListItemVO> latestContents;

    private List<CategoryVO> categories;

    private List<SourceVO> sources;

    private List<TagVO> tags;
}
