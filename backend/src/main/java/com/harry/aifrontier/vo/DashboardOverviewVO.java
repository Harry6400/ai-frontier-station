package com.harry.aifrontier.vo;

import com.harry.aifrontier.entity.FetchLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardOverviewVO {

    private TotalsVO totals;

    private List<StatItemVO> publishStatusStats;

    private List<StatItemVO> contentTypeStats;

    private List<StatItemVO> sourceTypeStats;

    private List<RecentContentVO> recentContents;

    private List<RecentExternalRefVO> recentExternalRefs;

    @Data
    public static class TotalsVO {

        private Long contents;

        private Long published;

        private Long drafts;

        private Long archived;

        private Long categories;

        private Long sources;

        private Long externalRefs;
    }

    @Data
    public static class StatItemVO {

        private String label;

        private String value;

        private Long count;

        private Integer ratio;
    }

    @Data
    public static class RecentContentVO {

        private Long id;

        private String title;

        private String contentType;

        private String publishStatus;

        private String categoryName;

        private String sourceName;

        private String sourceType;

        private Integer viewCount;

        private LocalDateTime publishedAt;

        private LocalDateTime updatedAt;
    }

    @Data
    public static class RecentExternalRefVO {

        private Long id;

        private Long contentId;

        private String contentTitle;

        private String refType;

        private String externalId;

        private String externalUrl;

        private LocalDateTime syncedAt;
    }

    private FetchLogStatsVO fetchLogStats;

    @Data
    public static class FetchLogStatsVO {

        /** 最近7天总采集次数 */
        private Long totalFetches7d;

        /** 成功次数 */
        private Long successfulFetches7d;

        /** 失败次数 */
        private Long failedFetches7d;

        /** 最近7天导入论文/内容数 */
        private Long totalPapersImported7d;

        /** 最近一次采集记录 */
        private FetchLog lastFetch;
    }
}
