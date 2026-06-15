package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 公众概览响应（§5.1 GET /api/public/dashboard）。
 * 仅返回当前公众用户本人的征集清单与下载记录。
 */
@Data
public class PublicDashboardResponse {

    private CollectionSummary collectionSummary;
    private List<IntakeBatchResponse> recentCollections;
    private List<ArchiveAccessLogResponse> downloadLogs;
    private OffsetDateTime summarizedAt;

    /** 本人征集清单按状态汇总。 */
    @Data
    public static class CollectionSummary {
        private long total;
        private long draft;
        private long inProgress;
        private long completed;
    }
}
