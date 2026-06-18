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
    private static final Map<String, String> MODULE_LABELS = Map.ofEntries(
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
            Map.entry("M14", "系统配置")
    );

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
        vo.setModuleLabel(MODULE_LABELS.get(l.getModuleName()));
        vo.setOperationType(l.getOperationType());
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
