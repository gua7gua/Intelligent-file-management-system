package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.entity.ArchiveAccessLog;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveAccessLogQueryService {

    private static final int EXPORT_MAX = 10000;

    private final ArchiveAccessLogMapper archiveAccessLogMapper;

    /** 23.2 查询档案访问日志（游标分页） */
    public CursorResult<ArchiveAccessLogResponse> query(ArchiveAccessLogQuery q) {
        int limit = q.getLimit() != null ? q.getLimit() : 20;
        QueryWrapper<ArchiveAccessLog> qw = baseFilters(q);
        applyCursor(qw, q.getCursor(), "accessed_at");
        qw.orderByDesc("accessed_at").orderByDesc("id").last("LIMIT " + (limit + 1));

        List<ArchiveAccessLog> rows = archiveAccessLogMapper.selectList(qw);
        boolean hasNext = rows.size() > limit;
        List<ArchiveAccessLog> page = hasNext ? rows.subList(0, limit) : rows;

        List<ArchiveAccessLogResponse> records = page.stream().map(this::toResponse).toList();
        String nextCursor = hasNext
                ? CursorCodec.encode(page.get(page.size() - 1).getAccessedAt(),
                        page.get(page.size() - 1).getId())
                : null;
        return new CursorResult<>(records, nextCursor, hasNext);
    }

    public List<ArchiveAccessLogResponse> listForExport(ArchiveAccessLogQuery q) {
        QueryWrapper<ArchiveAccessLog> qw = baseFilters(q);
        qw.orderByDesc("accessed_at").orderByDesc("id").last("LIMIT " + EXPORT_MAX);
        return archiveAccessLogMapper.selectList(qw).stream().map(this::toResponse).toList();
    }

    private QueryWrapper<ArchiveAccessLog> baseFilters(ArchiveAccessLogQuery q) {
        QueryWrapper<ArchiveAccessLog> qw = new QueryWrapper<>();
        if (q.getUserId() != null) qw.eq("user_id", q.getUserId());
        if (q.getArchiveId() != null) qw.eq("archive_id", q.getArchiveId());
        if (q.getAccessType() != null && !q.getAccessType().isBlank()) qw.eq("access_type", q.getAccessType());
        if (q.getStartedAt() != null) qw.ge("accessed_at", q.getStartedAt());
        if (q.getEndedAt() != null) qw.le("accessed_at", q.getEndedAt());
        return qw;
    }

    private void applyCursor(QueryWrapper<ArchiveAccessLog> qw, String cursor, String tsColumn) {
        CursorCodec.Decoded c = CursorCodec.decode(cursor);
        if (c == null) return;
        qw.and(w -> w.lt(tsColumn, c.timestamp())
                .or(o -> o.eq(tsColumn, c.timestamp()).lt("id", c.id())));
    }

    private ArchiveAccessLogResponse toResponse(ArchiveAccessLog l) {
        ArchiveAccessLogResponse vo = new ArchiveAccessLogResponse();
        vo.setId(l.getId());
        vo.setUserId(l.getUserId());
        vo.setUserType(l.getUserType());
        vo.setArchiveId(l.getArchiveId());
        vo.setArchiveFileId(l.getArchiveFileId());
        vo.setAccessType(l.getAccessType());
        vo.setIpAddress(l.getIpAddress());
        vo.setAccessedAt(l.getAccessedAt());
        return vo;
    }
}
