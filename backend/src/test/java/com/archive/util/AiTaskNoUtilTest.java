package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiTaskNoUtilTest {

    @Test
    void generate_序列号格式化为AIT前缀6位() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_ai_task_no')"), eq(Long.class)))
                .thenReturn(7L);

        AiTaskNoUtil util = new AiTaskNoUtil(jdbc);

        assertThat(util.generate()).isEqualTo("AIT-000007");
        verify(jdbc, times(1)).queryForObject(eq("SELECT nextval('seq_ai_task_no')"), eq(Long.class));
    }

    @Test
    void generate_序号超过6位仍正确显示() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1234567L);

        assertThat(new AiTaskNoUtil(jdbc).generate()).isEqualTo("AIT-1234567");
    }
}
