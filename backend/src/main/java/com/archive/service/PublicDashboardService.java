package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.PublicDashboardResponse;
import com.archive.dto.response.PublicDashboardResponse.CollectionSummary;
import com.archive.dto.response.PublicDashboardResponse.Stats;
import com.archive.dto.response.PublicDashboardResponse.User;
import com.archive.dto.response.PublicStatsResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveAccessLog;
import com.archive.entity.IntakeBatch;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公众概览聚合（§5.1 GET /api/public/dashboard）。
 * 全部按当前公众用户本人过滤：征集清单（public_user_id）、下载记录（archive_access_logs.user_id）。
 */
@Service
@RequiredArgsConstructor
public class PublicDashboardService {

    private static final int RECENT_LIMIT = 5;
    private static final int DOWNLOAD_LIMIT = 10;

    private final IntakeBatchService intakeBatchService;
    private final IntakeBatchMapper intakeBatchMapper;
    private final ArchiveAccessLogMapper archiveAccessLogMapper;
    private final ArchiveMapper archiveMapper;
    private final UserMapper userMapper;
    private final PublicStatsService publicStatsService;

    public PublicDashboardResponse overview() {
        long userId = AuthContext.getCurrentUserId();
        PublicDashboardResponse resp = new PublicDashboardResponse();
        CollectionSummary summary = buildSummary(userId);
        resp.setCollectionSummary(summary);
        resp.setRecentCollections(buildRecentCollections());
        resp.setDownloadLogs(buildDownloadLogs(userId));
        resp.setStats(buildStats(userId, summary));
        resp.setUser(buildUser(userId));
        resp.setSummarizedAt(OffsetDateTime.now());
        return resp;
    }

    private CollectionSummary buildSummary(long userId) {
        CollectionSummary s = new CollectionSummary();
        s.setTotal(countCollections(userId));
        s.setDraft(countByStatuses(userId, "draft"));
        s.setInProgress(countByStatuses(userId,
                "pending_contact", "pending_receive", "received", "partially_received"));
        s.setCompleted(countByStatuses(userId, "archived", "shelved"));
        return s;
    }

    private List<IntakeBatchResponse> buildRecentCollections() {
        BatchPageQuery q = new BatchPageQuery();
        q.setPageNo(1);
        q.setPageSize(RECENT_LIMIT);
        return intakeBatchService.listCollections(q).getRecords();
    }

    private List<ArchiveAccessLogResponse> buildDownloadLogs(long userId) {
        List<ArchiveAccessLog> logs = archiveAccessLogMapper.selectList(
                new QueryWrapper<ArchiveAccessLog>()
                        .eq("user_id", userId)
                        .eq("access_type", "download")
                        .orderByDesc("accessed_at").orderByDesc("id")
                        .last("LIMIT " + DOWNLOAD_LIMIT));
        if (logs.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Archive> archiveById = loadArchives(logs);
        List<ArchiveAccessLogResponse> out = new ArrayList<>();
        for (ArchiveAccessLog l : logs) {
            ArchiveAccessLogResponse r = new ArchiveAccessLogResponse();
            r.setId(l.getId());
            r.setUserId(l.getUserId());
            r.setUserType(l.getUserType());
            r.setArchiveId(l.getArchiveId());
            r.setArchiveFileId(l.getArchiveFileId());
            r.setAccessType(l.getAccessType());
            r.setIpAddress(l.getIpAddress());
            r.setAccessedAt(l.getAccessedAt());
            Archive a = l.getArchiveId() == null ? null : archiveById.get(l.getArchiveId());
            if (a != null) {
                r.setArchiveNo(a.getArchiveNo());
                r.setTitle(a.getTitle());
            }
            out.add(r);
        }
        return out;
    }

    /** 批量查询下载记录涉及的档案，避免 N+1。 */
    private Map<Long, Archive> loadArchives(List<ArchiveAccessLog> logs) {
        Set<Long> ids = logs.stream()
                .map(ArchiveAccessLog::getArchiveId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Archive> map = new HashMap<>();
        for (Archive a : archiveMapper.selectBatchIds(ids)) {
            map.put(a.getId(), a);
        }
        return map;
    }

    private Stats buildStats(long userId, CollectionSummary summary) {
        PublicStatsResponse fig = publicStatsService.publicFigures();
        Stats s = new Stats();
        s.setOpenArchiveCount(fig.getOpenArchiveCount());
        s.setElectronicFileCount(fig.getElectronicFileCount());
        s.setCollectionCount(fig.getCollectionCount());
        s.setLatestOpenCount(fig.getLatestOpenCount());
        s.setMyPendingCollections(summary.getInProgress());
        s.setMyDownloadCount(archiveAccessLogMapper.selectCount(
                new QueryWrapper<ArchiveAccessLog>()
                        .eq("user_id", userId)
                        .eq("access_type", "download")));
        return s;
    }

    private User buildUser(long userId) {
        User u = new User();
        com.archive.entity.User entity = userMapper.selectById(userId);
        if (entity == null) {
            return u;
        }
        u.setRealName(entity.getRealName());
        u.setPhone(entity.getPhone());
        u.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        return u;
    }

    private long countCollections(long userId) {
        return intakeBatchMapper.selectCount(new QueryWrapper<IntakeBatch>()
                .eq("public_user_id", userId).isNull("deleted_at"));
    }

    private long countByStatuses(long userId, String... statuses) {
        QueryWrapper<IntakeBatch> qw = new QueryWrapper<IntakeBatch>()
                .eq("public_user_id", userId).isNull("deleted_at");
        if (statuses.length == 1) {
            qw.eq("status", statuses[0]);
        } else {
            qw.in("status", (Object[]) statuses);
        }
        return intakeBatchMapper.selectCount(qw);
    }
}
