package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.PageResult;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.PublicDashboardResponse;
import com.archive.dto.response.PublicStatsResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveAccessLog;
import com.archive.entity.User;
import com.archive.enums.UserStatus;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicDashboardServiceTest {

    @Mock
    private IntakeBatchService intakeBatchService;
    @Mock
    private IntakeBatchMapper intakeBatchMapper;
    @Mock
    private ArchiveAccessLogMapper archiveAccessLogMapper;
    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PublicStatsService publicStatsService;

    @InjectMocks
    private PublicDashboardService service;

    @Test
    void overview_聚合本人征集下载统计与资料() {
        // buildSummary 四次 selectCount 顺序：total, draft, inProgress, completed
        when(intakeBatchMapper.selectCount(any())).thenReturn(10L, 2L, 5L, 3L);

        IntakeBatchResponse batch = new IntakeBatchResponse();
        batch.setId(1L);
        batch.setTitle("我的征集");
        when(intakeBatchService.listCollections(any(BatchPageQuery.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1, 5, 10L));

        ArchiveAccessLog dl = new ArchiveAccessLog();
        dl.setId(99L);
        dl.setUserId(100L);
        dl.setArchiveId(7L);
        dl.setAccessType("download");
        dl.setAccessedAt(OffsetDateTime.now());
        when(archiveAccessLogMapper.selectList(any())).thenReturn(List.of(dl));
        // myDownloadCount
        when(archiveAccessLogMapper.selectCount(any())).thenReturn(15L);

        Archive arch = new Archive();
        arch.setId(7L);
        arch.setArchiveNo("KLMY-2024-001");
        arch.setTitle("某档案题名");
        when(archiveMapper.selectBatchIds(anyCollection())).thenReturn(List.of(arch));

        User u = new User();
        u.setId(100L);
        u.setRealName("小周");
        u.setPhone("138****0005");
        u.setStatus(UserStatus.active);
        when(userMapper.selectById(100L)).thenReturn(u);

        PublicStatsResponse fig = new PublicStatsResponse();
        fig.setOpenArchiveCount(128L);
        fig.setElectronicFileCount(56L);
        fig.setCollectionCount(30L);
        fig.setLatestOpenCount(12L);
        when(publicStatsService.publicFigures()).thenReturn(fig);

        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(AuthContext::getCurrentUserId).thenReturn(100L);

            PublicDashboardResponse resp = service.overview();

            assertThat(resp.getCollectionSummary().getTotal()).isEqualTo(10L);
            assertThat(resp.getCollectionSummary().getInProgress()).isEqualTo(5L);

            assertThat(resp.getRecentCollections()).hasSize(1);
            assertThat(resp.getRecentCollections().get(0).getTitle()).isEqualTo("我的征集");

            assertThat(resp.getDownloadLogs()).hasSize(1);
            assertThat(resp.getDownloadLogs().get(0).getId()).isEqualTo(99L);
            assertThat(resp.getDownloadLogs().get(0).getArchiveNo()).isEqualTo("KLMY-2024-001");
            assertThat(resp.getDownloadLogs().get(0).getTitle()).isEqualTo("某档案题名");

            assertThat(resp.getStats().getOpenArchiveCount()).isEqualTo(128L);
            assertThat(resp.getStats().getElectronicFileCount()).isEqualTo(56L);
            assertThat(resp.getStats().getMyPendingCollections()).isEqualTo(5L);
            assertThat(resp.getStats().getMyDownloadCount()).isEqualTo(15L);

            assertThat(resp.getUser().getRealName()).isEqualTo("小周");
            assertThat(resp.getUser().getPhone()).isEqualTo("138****0005");
            assertThat(resp.getUser().getStatus()).isEqualTo("active");

            assertThat(resp.getSummarizedAt()).isNotNull();
        }
    }

    @Test
    void overview_无下载记录时不查档案且stats仍聚合() {
        when(intakeBatchMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L);
        when(intakeBatchService.listCollections(any(BatchPageQuery.class)))
                .thenReturn(new PageResult<>(List.of(), 1, 5, 0L));
        when(archiveAccessLogMapper.selectList(any())).thenReturn(List.of());
        when(archiveAccessLogMapper.selectCount(any())).thenReturn(0L);

        User u = new User();
        u.setRealName("小刘");
        u.setStatus(UserStatus.disabled);
        when(userMapper.selectById(200L)).thenReturn(u);
        when(publicStatsService.publicFigures()).thenReturn(new PublicStatsResponse());

        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(AuthContext::getCurrentUserId).thenReturn(200L);

            PublicDashboardResponse resp = service.overview();

            assertThat(resp.getDownloadLogs()).isEmpty();
            assertThat(resp.getStats().getMyDownloadCount()).isEqualTo(0L);
            assertThat(resp.getUser().getStatus()).isEqualTo("disabled");
        }
    }
}
