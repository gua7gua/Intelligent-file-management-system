package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class InternalDashboardResponse {
    /** 最近查阅（来自 archive_access_logs） */
    private List<RecentView> recentViews;
    /** 我的借阅申请 */
    private List<BorrowSummaryItem> myBorrowRequests;
    /** 当前借阅 */
    private List<BorrowSummaryItem> currentBorrows;
    /** 逾期提示 */
    private List<BorrowSummaryItem> overdueReminders;

    @Data
    @AllArgsConstructor
    public static class RecentView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private OffsetDateTime accessedAt;
    }
}
