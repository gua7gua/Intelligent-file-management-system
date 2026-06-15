package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理概览聚合响应（§6.1 GET /api/admin/dashboard）。
 * 字段逐一对齐前端 types/dashboard.ts 的 DashboardSummary 契约。
 */
@Data
public class AdminDashboardResponse {

    private DashboardTodos todos;
    private ArchiveSummary archiveSummary;
    private List<WarehouseWarning> warehouseWarnings;
    private List<RecentAuditLog> recentAuditLogs;
    private List<OverviewTodo> todoEntries;
    private OffsetDateTime summarizedAt;

    /** 概览待办计数（§6.1 todos）。 */
    @Data
    public static class DashboardTodos {
        private long pendingTransferReception;
        private long pendingArchive;
        private long borrowApproval;
        private long approvalPending;
        private long pendingDestruction;
        private long pendingShelf;
        private long appraisalDue;
        private long newBorrowRequests;
    }

    /** 馆藏汇总（概览指标卡）。 */
    @Data
    public static class ArchiveSummary {
        private long totalArchives;
        private long monthAdded;
        /** MinIO 已用 / 配额（0~1）。 */
        private double storageUsage;
        /** 存储告警阈值（0~1，来自 system_config）。 */
        private double storageWarningThreshold;
    }

    /** 库房告警项（复用 WarehouseService.listWarningRooms）。 */
    @Data
    public static class WarehouseWarning {
        private String roomNo;
        private String roomName;
        /** 占用率（0~1）。 */
        private double occupancyRate;
        private double warningThreshold;
    }

    /** 最近操作日志行（recentAuditLogs，join users 取操作人名）。 */
    @Data
    public static class RecentAuditLog {
        private Long id;
        /** 操作人姓名（system 类型显示"系统"）。 */
        private String operator;
        private String module;
        private String action;
        private OffsetDateTime operatedAt;
    }

    /** 待办提醒项（原型 todo-list，跳转入口）。 */
    @Data
    public static class OverviewTodo {
        private String key;
        private String title;
        private String description;
        private long count;
        private String targetRoute;
        private Map<String, String> targetQuery;
        /** info / warning / danger。 */
        private String severity;
    }
}
