package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.StatisticsOverviewQuery;
import com.archive.dto.response.StatisticsOverviewResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.util.PdfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    private StatisticsService service;
    private JdbcTemplate jdbcTemplate;
    private PdfGenerator pdfGenerator;

    @BeforeEach
    void setup() {
        jdbcTemplate = mock(JdbcTemplate.class);
        pdfGenerator = mock(PdfGenerator.class);
        service = new StatisticsService(jdbcTemplate, pdfGenerator);
    }

    private void asBack(Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(true);
            body.run();
        }
    }

    private void stubAggregates(long count) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(count);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());
    }

    @Test
    void overview_返回四项总量() {
        stubAggregates(5L);
        asBack(() -> {
            StatisticsOverviewResponse o = service.overview(new StatisticsOverviewQuery());
            assertThat(o.getTotals().getTotalArchives()).isEqualTo(5L);
            assertThat(o.getTotals().getOpenArchives()).isEqualTo(5L);
            assertThat(o.getTrends()).isEmpty();
            assertThat(o.getStorageUsage()).isEmpty();
        });
    }

    @Test
    void export_pdf调用pdfGenerator() {
        stubAggregates(1L);
        byte[] pdf = new byte[]{1, 2, 3};
        when(pdfGenerator.generateStatisticsPdf(any(), any())).thenReturn(pdf);
        StatisticsOverviewQuery q = new StatisticsOverviewQuery();
        q.setFormat("pdf");
        asBack(() -> {
            byte[] out = service.export(q);
            assertThat(out).containsExactly(1, 2, 3);
        });
        verify(pdfGenerator).generateStatisticsPdf(any(), any());
    }

    @Test
    void export_xlsx返回非空字节() {
        stubAggregates(2L);
        StatisticsOverviewQuery q = new StatisticsOverviewQuery();
        q.setFormat("xlsx");
        asBack(() -> assertThat(service.export(q).length).isPositive());
    }

    @Test
    void export_非法format抛BAD_REQUEST() {
        stubAggregates(1L);
        StatisticsOverviewQuery q = new StatisticsOverviewQuery();
        q.setFormat("csv");
        asBack(() -> assertThatThrownBy(() -> service.export(q))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("格式"));
    }
}
