package com.archive.service;

import com.archive.dto.response.AdminDashboardResponse;
import com.archive.dto.response.AdminDashboardResponse.ArchiveSummary;
import com.archive.dto.response.AdminDashboardResponse.DashboardTodos;
import com.archive.dto.response.AdminDashboardResponse.OverviewTodo;
import com.archive.dto.response.AdminDashboardResponse.RecentAuditLog;
import com.archive.dto.response.AdminDashboardResponse.WarehouseWarning;
import com.archive.dto.response.WarehouseWarningResponse;
import com.archive.entity.AppraisalBatch;
import com.archive.entity.Archive;
import com.archive.entity.AuditLog;
import com.archive.entity.BorrowRequest;
import com.archive.entity.DestructionList;
import com.archive.entity.IntakeBatch;
import com.archive.entity.SystemConfig;
import com.archive.entity.User;
import com.archive.entity.ApprovalRequest;
import com.archive.enums.AppraisalBatchStatus;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.BatchStatus;
import com.archive.enums.BorrowStatus;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.AuditLogMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.SystemConfigMapper;
import com.archive.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理概览聚合（§6.1 GET /api/admin/dashboard）。
 * 各待办计数按业务表 status 枚举 name() 统计；存储用量取 archive_files 总字节 / 配置配额。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final long DEFAULT_QUOTA_BYTES = 500L * 1024 * 1024 * 1024; // 500GB
    private static final double DEFAULT_STORAGE_THRESHOLD = 0.85;

    private final IntakeBatchMapper intakeBatchMapper;
    private final BorrowRequestMapper borrowRequestMapper;
    private final ApprovalRequestMapper approvalRequestMapper;
    private final DestructionListMapper destructionListMapper;
    private final ArchiveMapper archiveMapper;
    private final AppraisalBatchMapper appraisalBatchMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final WarehouseService warehouseService;

    public AdminDashboardResponse overview() {
        AdminDashboardResponse resp = new AdminDashboardResponse();
        DashboardTodos todos = buildTodos();
        resp.setTodos(todos);
        ArchiveSummary summary = buildArchiveSummary();
        resp.setArchiveSummary(summary);
        resp.setWarehouseWarnings(buildWarehouseWarnings());
        resp.setRecentAuditLogs(buildRecentAuditLogs());
        resp.setTodoEntries(buildTodoEntries(todos, summary));
        resp.setSummarizedAt(OffsetDateTime.now());
        return resp;
    }

    private DashboardTodos buildTodos() {
        DashboardTodos t = new DashboardTodos();
        // 概览「待验收移交清单」统计已提交、尚未接收的移交/征集批次
        // （submit 后状态由 draft 变为 pending_transfer，进入等待接收阶段）
        t.setPendingTransferReception(intakeBatchMapper.selectCount(
                new QueryWrapper<IntakeBatch>().eq("status", BatchStatus.pending_transfer.name())
                        .isNull("deleted_at")));
        t.setPendingArchive(intakeBatchMapper.selectCount(
                new QueryWrapper<IntakeBatch>().in("status",
                        BatchStatus.received.name(), BatchStatus.partially_received.name())
                        .isNull("deleted_at")));
        long borrowApplied = borrowRequestMapper.selectCount(
                new QueryWrapper<BorrowRequest>().eq("status", BorrowStatus.applied.name())
                        .isNull("deleted_at"));
        t.setBorrowApproval(borrowApplied);
        t.setNewBorrowRequests(borrowApplied);
        t.setApprovalPending(approvalRequestMapper.selectCount(
                new QueryWrapper<ApprovalRequest>().eq("status", ApprovalStatus.pending.name())));
        t.setPendingDestruction(destructionListMapper.selectCount(
                new QueryWrapper<DestructionList>().in("status",
                        DestructionListStatus.pending_approval.name(),
                        DestructionListStatus.pending_destroy.name())));
        t.setPendingShelf(archiveMapper.selectCount(
                new QueryWrapper<Archive>().eq("lifecycle_status", LifecycleStatus.pending_shelf.name())));
        t.setAppraisalDue(appraisalBatchMapper.selectCount(
                new QueryWrapper<AppraisalBatch>().eq("status", AppraisalBatchStatus.draft.name())));
        return t;
    }

    private ArchiveSummary buildArchiveSummary() {
        ArchiveSummary s = new ArchiveSummary();
        s.setTotalArchives(archiveMapper.selectCount(new QueryWrapper<>()));
        OffsetDateTime monthStart = OffsetDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        s.setMonthAdded(archiveMapper.selectCount(
                new QueryWrapper<Archive>().ge("created_at", monthStart)));
        long usedBytes = archiveFileMapper.sumFileSize();
        long quota = readConfigLong("storage.quota.bytes", DEFAULT_QUOTA_BYTES);
        s.setStorageUsage(quota > 0 ? Math.min(1.0, (double) usedBytes / (double) quota) : 0.0);
        s.setStorageWarningThreshold(readConfigDouble("storage.warning.threshold", DEFAULT_STORAGE_THRESHOLD));
        return s;
    }

    private List<WarehouseWarning> buildWarehouseWarnings() {
        List<WarehouseWarningResponse> rooms = warehouseService.listWarningRooms();
        List<WarehouseWarning> out = new ArrayList<>();
        for (WarehouseWarningResponse r : rooms) {
            WarehouseWarning w = new WarehouseWarning();
            w.setRoomNo(r.getRoomNo());
            w.setRoomName(r.getRoomName());
            w.setOccupancyRate(r.getOccupancyRate() != null ? r.getOccupancyRate().doubleValue() : 0.0);
            w.setWarningThreshold(r.getWarningThreshold() != null ? r.getWarningThreshold().doubleValue() : 0.0);
            out.add(w);
        }
        return out;
    }

    private List<RecentAuditLog> buildRecentAuditLogs() {
        List<AuditLog> logs = auditLogMapper.selectList(new QueryWrapper<AuditLog>()
                .orderByDesc("operated_at").orderByDesc("id").last("LIMIT 5"));
        if (logs.isEmpty()) {
            return List.of();
        }
        // 操作人姓名：system 显示"系统"，其余按 actorUserId 查 users.realName
        Set<Long> userIds = logs.stream()
                .filter(l -> !"system".equals(l.getActorType()) && l.getActorUserId() != null)
                .map(AuditLog::getActorUserId)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                nameMap.put(u.getId(), u.getRealName());
            }
        }
        List<RecentAuditLog> out = new ArrayList<>();
        for (AuditLog l : logs) {
            RecentAuditLog r = new RecentAuditLog();
            r.setId(l.getId());
            if ("system".equals(l.getActorType())) {
                r.setOperator("系统");
            } else if (l.getActorUserId() != null) {
                r.setOperator(nameMap.getOrDefault(l.getActorUserId(), "用户#" + l.getActorUserId()));
            } else {
                r.setOperator("未知");
            }
            r.setModule(l.getModuleName());
            // 复用 AuditLogQueryService 的模块/操作类型中文映射，补富字段
            r.setModuleLabel(AuditLogQueryService.moduleLabel(l.getModuleName()));
            r.setAction(l.getOperationType());
            r.setActionLabel(AuditLogQueryService.operationLabel(l.getOperationType()));
            r.setOperatedAt(l.getOperatedAt());
            out.add(r);
        }
        return out;
    }

    private List<OverviewTodo> buildTodoEntries(DashboardTodos todos, ArchiveSummary summary) {
        List<OverviewTodo> entries = new ArrayList<>();
        entries.add(todo("pending_transfer", "待验收移交清单", "前台核对实物、上传电子文件、导出回执",
                todos.getPendingTransferReception(), "/admin/transfer-reception",
                Map.of("status", "pending_transfer"), "warning"));
        entries.add(todo("pending_archive", "待 AI 补全与入库", "已接收条目等待后台确认字段",
                todos.getPendingArchive(), "/admin/pending-archive",
                Map.of("status", "accepted"), "info"));
        entries.add(todo("pending_shelf", "待上架档案", "已入库未上架，暂不向查阅者开放",
                todos.getPendingShelf(), "/admin/pending-archive",
                Map.of("status", "pending_shelf"), "warning"));
        // 存储告警：count 用百分比整数
        int storagePct = (int) Math.round(summary.getStorageUsage() * 100);
        entries.add(todo("storage_warning", "存储空间告警", "MinIO 使用率超过系统配置阈值",
                storagePct, "/admin/preservation", null,
                summary.getStorageUsage() >= summary.getStorageWarningThreshold() ? "danger" : "info"));
        return entries;
    }

    private OverviewTodo todo(String key, String title, String description, long count,
                              String targetRoute, Map<String, String> targetQuery, String severity) {
        OverviewTodo o = new OverviewTodo();
        o.setKey(key);
        o.setTitle(title);
        o.setDescription(description);
        o.setCount(count);
        o.setTargetRoute(targetRoute);
        o.setTargetQuery(targetQuery);
        o.setSeverity(severity);
        return o;
    }

    private double readConfigDouble(String key, double def) {
        SystemConfig c = systemConfigMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key));
        if (c == null || c.getConfigValue() == null) {
            return def;
        }
        try {
            return Double.parseDouble(c.getConfigValue());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long readConfigLong(String key, long def) {
        SystemConfig c = systemConfigMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key));
        if (c == null || c.getConfigValue() == null) {
            return def;
        }
        try {
            return Long.parseLong(c.getConfigValue());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
