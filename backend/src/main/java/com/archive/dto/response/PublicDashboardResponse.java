package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 公众概览响应（§5.1 GET /api/public/dashboard）。
 * 返回当前公众用户本人的征集清单、下载记录、公开馆藏统计与基础资料。
 */
@Data
public class PublicDashboardResponse {

    private CollectionSummary collectionSummary;
    private List<IntakeBatchResponse> recentCollections;
    private List<ArchiveAccessLogResponse> downloadLogs;
    private Stats stats;
    private User user;
    private OffsetDateTime summarizedAt;

    /** 本人征集清单按状态汇总。 */
    @Data
    public static class CollectionSummary {
        private long total;
        private long draft;
        private long inProgress;
        private long completed;
    }

    /** 公开馆藏统计 + 本人统计。 */
    @Data
    public static class Stats {
        private long openArchiveCount;
        private long electronicFileCount;
        private long collectionCount;
        private long latestOpenCount;
        private long myPendingCollections;
        private long myDownloadCount;
    }

    /** 当前公众用户基础资料。 */
    @Data
    public static class User {
        private String realName;
        private String phone;
        /** active | disabled */
        private String status;
    }
}
