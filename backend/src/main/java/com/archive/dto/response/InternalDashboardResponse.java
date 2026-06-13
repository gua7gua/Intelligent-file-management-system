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
    /** 我的借阅申请 —— 本次返回空，待 feat/borrow-liu 补齐 */
    private List<Object> myBorrowRequests;
    /** 当前借阅 —— 本次返回空 */
    private List<Object> currentBorrows;
    /** 逾期提示 —— 本次返回空 */
    private List<Object> overdueReminders;

    @Data
    @AllArgsConstructor
    public static class RecentView {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private OffsetDateTime accessedAt;
    }
}
