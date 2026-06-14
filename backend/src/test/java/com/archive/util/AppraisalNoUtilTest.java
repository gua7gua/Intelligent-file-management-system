package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppraisalNoUtilTest {

    @Test
    void generate_使用序列生成APP加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_appraisal_batch_no')"), eq(Long.class)))
                .thenReturn(1L);

        assertThat(new AppraisalNoUtil(jdbc).generate()).isEqualTo("APP-000001");
    }

    @Test
    void generate_大序号正确补零() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_appraisal_batch_no')"), eq(Long.class)))
                .thenReturn(23456L);

        assertThat(new AppraisalNoUtil(jdbc).generate()).isEqualTo("APP-023456");
    }
}
