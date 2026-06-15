package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.PageResult;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.PublicDashboardResponse;
import com.archive.entity.ArchiveAccessLog;
import com.archive.mapper.ArchiveAccessLogMapper;
import com.archive.mapper.IntakeBatchMapper;
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

    @InjectMocks
    private PublicDashboardService service;

    @Test
    void overview_按当前公众用户聚合() {
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
        dl.setAccessType("download");
        dl.setAccessedAt(OffsetDateTime.now());
        when(archiveAccessLogMapper.selectList(any())).thenReturn(List.of(dl));

        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(AuthContext::getCurrentUserId).thenReturn(100L);

            PublicDashboardResponse resp = service.overview();

            assertThat(resp.getCollectionSummary().getTotal()).isEqualTo(10L);
            assertThat(resp.getCollectionSummary().getDraft()).isEqualTo(2L);
            assertThat(resp.getCollectionSummary().getInProgress()).isEqualTo(5L);
            assertThat(resp.getCollectionSummary().getCompleted()).isEqualTo(3L);

            assertThat(resp.getRecentCollections()).hasSize(1);
            assertThat(resp.getRecentCollections().get(0).getTitle()).isEqualTo("我的征集");

            assertThat(resp.getDownloadLogs()).hasSize(1);
            assertThat(resp.getDownloadLogs().get(0).getId()).isEqualTo(99L);
            assertThat(resp.getSummarizedAt()).isNotNull();
        }
    }
}
