package com.archive.service;

import com.archive.common.CursorCodec;
import com.archive.common.CursorResult;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveAccessLog;
import com.archive.entity.User;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveMapper;
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
public class ArchiveAccessLogQueryService {

    private final ArchiveAccessLogMapper archiveAccessLogMapper;
    private final UserMapper userMapper;
    private final ArchiveMapper archiveMapper;

    /** 23.2 查询档案访问日志（游标分页） */
    public CursorResult<ArchiveAccessLogResponse> query(ArchiveAccessLogQuery q) {
        int limit = q.getLimit() != null ? q.getLimit() : 20;
        QueryWrapper<ArchiveAccessLog> qw = baseFilters(q);
        applyCursor(qw, q.getCursor(), "accessed_at");
        qw.orderByDesc("accessed_at").orderByDesc("id").last("LIMIT " + (limit + 1));

        List<ArchiveAccessLog> rows = archiveAccessLogMapper.selectList(qw);
        boolean hasNext = rows.size() > limit;
        List<ArchiveAccessLog> page = hasNext ? rows.subList(0, limit) : rows;

        // 批量预取档案信息和访问人姓名，避免 N+1
        Map<Long, Archive> archiveById = loadArchiveMap(page);
        Map<Long, String> userNameById = loadUserNameMap(page);

        List<ArchiveAccessLogResponse> records = page.stream()
                .map(l -> toResponse(l, archiveById, userNameById)).toList();
        String nextCursor = hasNext
                ? CursorCodec.encode(page.get(page.size() - 1).getAccessedAt(),
                        page.get(page.size() - 1).getId())
                : null;
        return new CursorResult<>(records, nextCursor, hasNext);
    }

    private Map<Long, Archive> loadArchiveMap(List<ArchiveAccessLog> rows) {
        Set<Long> archiveIds = rows.stream()
                .map(ArchiveAccessLog::getArchiveId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Archive> map = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                map.put(a.getId(), a);
            }
        }
        return map;
    }

    private Map<Long, String> loadUserNameMap(List<ArchiveAccessLog> rows) {
        Set<Long> userIds = rows.stream()
                .map(ArchiveAccessLog::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                map.put(u.getId(), u.getRealName());
            }
        }
        return map;
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

    private ArchiveAccessLogResponse toResponse(ArchiveAccessLog l,
                                                Map<Long, Archive> archiveById,
                                                Map<Long, String> userNameById) {
        ArchiveAccessLogResponse vo = new ArchiveAccessLogResponse();
        vo.setId(l.getId());
        vo.setUserId(l.getUserId());
        vo.setUserType(l.getUserType());
        vo.setArchiveId(l.getArchiveId());
        vo.setArchiveFileId(l.getArchiveFileId());
        vo.setAccessType(l.getAccessType());
        vo.setIpAddress(l.getIpAddress());
        vo.setAccessedAt(l.getAccessedAt());
        if (l.getArchiveId() != null) {
            Archive a = archiveById.get(l.getArchiveId());
            if (a != null) {
                vo.setArchiveNo(a.getArchiveNo());
                vo.setTitle(a.getTitle());
            }
        }
        if (l.getUserId() != null) {
            vo.setActorName(userNameById.getOrDefault(l.getUserId(), "用户#" + l.getUserId()));
        } else if ("anonymous".equals(l.getUserType())) {
            vo.setActorName("匿名公众");
        }
        return vo;
    }
}
