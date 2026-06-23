package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalysisTaskNoUtilTest {

    @Test
    void generate_格式与序列一致() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_analysis_task_no')"), eq(Long.class)))
                .thenReturn(3L);
        // 编号格式为 ANA-YYYYMM-NNNN（V24 起含年月），序列零填充至 4 位
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        assertThat(new AnalysisTaskNoUtil(jdbc).generate()).isEqualTo("ANA-" + ym + "-0003");
    }
}
