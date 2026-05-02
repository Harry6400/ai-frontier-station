package com.harry.aifrontier.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArxivPaperCandidateVO {

    private String arxivId;

    private String title;

    private List<String> authors;

    private String abstractText;

    private String pdfUrl;

    private String htmlUrl;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;

    private List<String> categories;

    private String primaryCategory;
}
