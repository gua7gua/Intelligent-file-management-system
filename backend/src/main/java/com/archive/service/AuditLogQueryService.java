package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.entity.AuditLog;
import com.archive.mapper.AuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogMapper auditLogMapper;

    /** 23.1 查询审计日志（游标分页） */
    public CursorResult<AuditLogResponse> query(AuditLogQuery q) {
        int limit = q.getLimit() != null ? q.getLimit() : 20;
        QueryWrapper<AuditLog> qw = baseFilters(q);
        applyCursor(qw, q.getCursor(), "operated_at");
        qw.orderByDesc("operated_at").orderByDesc("id").last("LIMIT " + (limit + 1));

        List<AuditLog> rows = auditLogMapper.selectList(qw);
        boolean hasNext = rows.size() > limit;
        List<AuditLog> page = hasNext ? rows.subList(0, limit) : rows;

        List<AuditLogResponse> records = page.stream().map(this::toResponse).toList();
        String nextCursor = null;
        if (hasNext) {
            AuditLog last = page.get(page.size() - 1);
            nextCursor = CursorCodec.encode(last.getOperatedAt(), last.getId());
        }
        return new CursorResult<>(records, nextCursor, hasNext);
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

    private AuditLogResponse toResponse(AuditLog l) {
        AuditLogResponse vo = new AuditLogResponse();
        vo.setId(l.getId());
        vo.setActorUserId(l.getActorUserId());
        vo.setActorType(l.getActorType());
        vo.setModuleName(l.getModuleName());
        vo.setOperationType(l.getOperationType());
        vo.setBusinessType(l.getBusinessType());
        vo.setBusinessId(l.getBusinessId());
        vo.setDetail(l.getDetail());
        vo.setIpAddress(l.getIpAddress());
        vo.setOperatedAt(l.getOperatedAt());
        return vo;
    }
}
