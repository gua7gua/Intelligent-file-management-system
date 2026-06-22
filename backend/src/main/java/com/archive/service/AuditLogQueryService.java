package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.entity.Archive;
import com.archive.entity.AuditLog;
import com.archive.entity.User;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.AuditLogMapper;
import com.archive.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final ArchiveMapper archiveMapper;

    /** 审计日志模块代号 -> 中文标签（与各 Service auditService.log 第一参一致）。 */
    public static final Map<String, String> MODULE_LABELS = Map.ofEntries(
            Map.entry("M01", "用户/密码"),
            Map.entry("M02", "登录认证"),
            Map.entry("M03", "电子文件上传"),
            Map.entry("M04", "AI 补全"),
            Map.entry("M05", "电子文件预览"),
            Map.entry("M06", "全宗管理"),
            Map.entry("M07", "库房管理"),
            Map.entry("M08", "审批管理"),
            Map.entry("M09", "借阅管理"),
            Map.entry("M10", "档案鉴定"),
            Map.entry("M11", "销毁管理"),
            Map.entry("M12", "盘点管理"),
            Map.entry("M13", "档案编研"),
            Map.entry("M14", "系统配置"),
            // V16 种子审计日志用裸英文模块名（运行时改用 M01-M14 代号），保留映射保证种子数据也能显示中文
            Map.entry("transfer", "移交管理"),
            Map.entry("transfer_reception", "移交接收"),
            Map.entry("pending_archive", "待入库"),
            Map.entry("borrow", "借阅管理"),
            Map.entry("approval", "审批管理"),
            Map.entry("system_config", "系统配置")
    );

    /** 审计日志 operation_type -> 中文标签（覆盖各 Service auditService.log 第二参的全部取值）。 */
    public static final Map<String, String> OPERATION_LABELS = Map.ofEntries(
            Map.entry("apply", "申请"),
            Map.entry("approve", "审批通过"),
            Map.entry("reject", "审批驳回"),
            Map.entry("register", "注册"),
            Map.entry("reset_password", "重置密码"),
            Map.entry("create_user", "创建用户"),
            Map.entry("update_user", "更新用户"),
            Map.entry("update_user_status", "更新用户状态"),
            Map.entry("delete", "删除"),
            Map.entry("upload", "上传"),
            Map.entry("download", "下载"),
            Map.entry("preview", "预览"),
            Map.entry("delete_file", "删除文件"),
            Map.entry("trigger_file_check", "触发文件校验"),
            Map.entry("start_ai_completion", "启动 AI 补全"),
            Map.entry("retry_ai_failed", "重试失败 AI 任务"),
            Map.entry("confirm_fields", "确认字段"),
            Map.entry("create_fonds", "创建全宗"),
            Map.entry("update_fonds", "更新全宗"),
            Map.entry("create_room", "创建库房"),
            Map.entry("create_box", "创建盒"),
            Map.entry("move_box", "移动盒"),
            Map.entry("update_location_status", "更新位置状态"),
            Map.entry("submit_security_adjustment", "提交密级调整"),
            Map.entry("submit_open_adjustment", "提交开放调整"),
            Map.entry("submit_destruction_approval", "提交销毁审批"),
            Map.entry("checkout", "借阅出库"),
            Map.entry("return", "归还"),
            Map.entry("issue_voucher", "发放凭证"),
            Map.entry("create_appraisal_batch", "创建鉴定批次"),
            Map.entry("save_appraisal_items", "保存鉴定项"),
            Map.entry("complete_appraisal", "完成鉴定"),
            Map.entry("destroy", "销毁"),
            Map.entry("upload_destruction_photo", "上传销毁照片"),
            Map.entry("create_inventory_task", "创建盘点任务"),
            Map.entry("start_inventory", "开始盘点"),
            Map.entry("complete_inventory", "完成盘点"),
            Map.entry("scan_failed", "扫描失败"),
            Map.entry("scan_reject", "扫描驳回"),
            Map.entry("shelve_batch", "批次上架"),
            Map.entry("create_analysis_task", "创建研判任务"),
            Map.entry("handle_analysis_item", "处理研判项"),
            Map.entry("generate_compilation", "生成编研"),
            Map.entry("archive_compilation", "编研入库"),
            Map.entry("confirm_archive", "确认入库"),
            Map.entry("create_backup", "创建备份"),
            Map.entry("update_metadata", "更新元数据"),
            Map.entry("create_organization", "创建组织"),
            Map.entry("update_organization", "更新组织"),
            Map.entry("delete_organization", "删除组织"),
            Map.entry("update_config", "更新配置"),
            Map.entry("delete_user", "删除用户"),
            // V16 种子审计日志用裸英文操作名（与运行时 M-code 短词不同），单独补中文
            Map.entry("submit_batch", "提交清单"),
            Map.entry("accept_batch", "接收清单"),
            Map.entry("archive_items", "条目入库"),
            Map.entry("apply_borrow", "申请借阅"),
            Map.entry("approve_destruction", "审批销毁"),
            // P1-2 补全：删除类操作 + 取消借阅（全集核对后缺失项）
            Map.entry("delete_fonds", "删除全宗"),
            Map.entry("delete_box", "删除盒"),
            Map.entry("delete_room", "删除库房"),
            Map.entry("delete_inventory_task", "删除盘点任务"),
            Map.entry("delete_appraisal_batch", "删除鉴定批次"),
            Map.entry("delete_analysis_item", "删除研判项"),
            Map.entry("cancel_borrow", "取消借阅")
    );

    /** 取模块中文标签，未知代号原样返回。 */
    public static String moduleLabel(String moduleCode) {
        if (moduleCode == null) {
            return null;
        }
        return MODULE_LABELS.getOrDefault(moduleCode, moduleCode);
    }

    /** 取操作类型中文标签，未知类型原样返回。 */
    public static String operationLabel(String operationType) {
        if (operationType == null) {
            return null;
        }
        return OPERATION_LABELS.getOrDefault(operationType, operationType);
    }

    /** 23.1 查询审计日志（游标分页） */
    public CursorResult<AuditLogResponse> query(AuditLogQuery q) {
        int limit = q.getLimit() != null ? q.getLimit() : 20;
        QueryWrapper<AuditLog> qw = baseFilters(q);
        applyCursor(qw, q.getCursor(), "operated_at");
        qw.orderByDesc("operated_at").orderByDesc("id").last("LIMIT " + (limit + 1));

        List<AuditLog> rows = auditLogMapper.selectList(qw);
        boolean hasNext = rows.size() > limit;
        List<AuditLog> page = hasNext ? rows.subList(0, limit) : rows;

        // 批量预取操作人姓名与关联档号，避免 toResponse 内逐条查询导致 N+1
        Map<Long, String> userNameById = loadUserNameMap(page);
        Map<Long, String> archiveNoById = loadArchiveNoMap(page);

        List<AuditLogResponse> records = page.stream()
                .map(l -> toResponse(l, userNameById, archiveNoById)).toList();
        String nextCursor = null;
        if (hasNext) {
            AuditLog last = page.get(page.size() - 1);
            nextCursor = CursorCodec.encode(last.getOperatedAt(), last.getId());
        }
        return new CursorResult<>(records, nextCursor, hasNext);
    }

    private Map<Long, String> loadUserNameMap(List<AuditLog> rows) {
        Set<Long> userIds = rows.stream()
                .filter(l -> !"system".equals(l.getActorType()) && l.getActorUserId() != null)
                .map(AuditLog::getActorUserId)
                .collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                map.put(u.getId(), u.getRealName());
            }
        }
        return map;
    }

    private Map<Long, String> loadArchiveNoMap(List<AuditLog> rows) {
        Set<Long> archiveIds = rows.stream()
                .filter(l -> "archive".equals(l.getBusinessType()) && l.getBusinessId() != null)
                .map(AuditLog::getBusinessId)
                .collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                map.put(a.getId(), a.getArchiveNo());
            }
        }
        return map;
    }

    private QueryWrapper<AuditLog> baseFilters(AuditLogQuery q) {
        QueryWrapper<AuditLog> qw = new QueryWrapper<>();
        if (q.getActorUserId() != null) qw.eq("actor_user_id", q.getActorUserId());
        if (q.getActorType() != null && !q.getActorType().isBlank()) qw.eq("actor_type", q.getActorType());
        if (q.getModuleName() != null && !q.getModuleName().isBlank()) qw.eq("module_name", q.getModuleName());
        if (q.getOperationType() != null && !q.getOperationType().isBlank()) qw.eq("operation_type", q.getOperationType());
        if (q.getBusinessType() != null && !q.getBusinessType().isBlank()) qw.eq("business_type", q.getBusinessType());
        if (q.getBusinessId() != null) qw.eq("business_id", q.getBusinessId());
        if (q.getStartedAt() != null) qw.ge("operated_at", q.getStartedAt());
        if (q.getEndedAt() != null) qw.le("operated_at", q.getEndedAt());
        return qw;
    }

    private void applyCursor(QueryWrapper<AuditLog> qw, String cursor, String tsColumn) {
        CursorCodec.Decoded c = CursorCodec.decode(cursor);
        if (c == null) return;
        qw.and(w -> w.lt(tsColumn, c.timestamp())
                .or(o -> o.eq(tsColumn, c.timestamp()).lt("id", c.id())));
    }

    private AuditLogResponse toResponse(AuditLog l, Map<Long, String> userNameById,
                                        Map<Long, String> archiveNoById) {
        AuditLogResponse vo = new AuditLogResponse();
        vo.setId(l.getId());
        vo.setActorUserId(l.getActorUserId());
        vo.setActorType(l.getActorType());
        vo.setModuleName(l.getModuleName());
        vo.setModuleLabel(moduleLabel(l.getModuleName()));
        vo.setOperationType(l.getOperationType());
        vo.setOperationLabel(operationLabel(l.getOperationType()));
        vo.setBusinessType(l.getBusinessType());
        vo.setBusinessId(l.getBusinessId());
        vo.setDetail(l.getDetail());
        vo.setIpAddress(l.getIpAddress());
        vo.setOperatedAt(l.getOperatedAt());
        // 操作人：system 显示「系统」，其余按 user_id 取 real_name，缺失则用 id 兜底
        if ("system".equals(l.getActorType())) {
            vo.setActorName("系统");
        } else if (l.getActorUserId() != null) {
            vo.setActorName(userNameById.getOrDefault(
                    l.getActorUserId(), "用户#" + l.getActorUserId()));
        }
        // 档号：仅当业务对象为档案时填充
        if ("archive".equals(l.getBusinessType()) && l.getBusinessId() != null) {
            String archiveNo = archiveNoById.get(l.getBusinessId());
            if (archiveNo != null) {
                vo.setArchiveNo(archiveNo);
            } else if (l.getDetail() != null && l.getDetail().get("archiveNo") instanceof String no) {
                // 联表未命中（如档案已删）时，尝试从 detail 兜底
                vo.setArchiveNo(no);
            }
        }
        return vo;
    }
}
