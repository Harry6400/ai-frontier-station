package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HuggingFacePaperCandidateVO {

    private String paperId;

    private String title;

    private List<String> authors;

    private String abstractText;

    private String htmlUrl;

    private LocalDateTime publishedAt;

    private Integer likes;

    private Integer comments;
}
