package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.PublicDashboardResponse;
import com.archive.dto.response.PublicDashboardResponse.CollectionSummary;
import com.archive.entity.ArchiveAccessLog;
import com.archive.entity.IntakeBatch;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public PublicDashboardResponse overview() {
        long userId = AuthContext.getCurrentUserId();
        PublicDashboardResponse resp = new PublicDashboardResponse();
        resp.setCollectionSummary(buildSummary(userId));
        resp.setRecentCollections(buildRecentCollections());
        resp.setDownloadLogs(buildDownloadLogs(userId));
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
            out.add(r);
        }
        return out;
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
