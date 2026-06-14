package com.archive.service;

import com.archive.dto.response.BorrowSummaryItem;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.mapper.ArchiveAccessLogMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceDashboardTest {

    @Test
    void getInternalDashboard_接入BorrowService填充三列表() {
        ArchiveAccessLogMapper accessLogMapper = mock(ArchiveAccessLogMapper.class);
        when(accessLogMapper.findRecentViews(4L)).thenReturn(List.of());

        BorrowService borrowService = mock(BorrowService.class);
        BorrowSummaryItem mine = new BorrowSummaryItem(
                "BRW-1", 2L, "ARC-1", "测试档案", "applied", null, null);
        when(borrowService.dashboardMine(4L, 5)).thenReturn(List.of(mine));
        when(borrowService.dashboardCurrent(4L, 5)).thenReturn(List.of());
        when(borrowService.dashboardOverdue(4L, 5)).thenReturn(List.of());

        SearchService service = new SearchService(null, null, accessLogMapper,
                null, null, null, null, null, borrowService);

        InternalDashboardResponse resp = service.getInternalDashboard(4L);
        assertThat(resp.getMyBorrowRequests()).hasSize(1);
        assertThat(resp.getMyBorrowRequests().get(0).getRequestNo()).isEqualTo("BRW-1");
        assertThat(resp.getCurrentBorrows()).isEmpty();
        assertThat(resp.getOverdueReminders()).isEmpty();
    }
}
