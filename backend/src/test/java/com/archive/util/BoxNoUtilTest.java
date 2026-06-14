package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoxNoUtilTest {

    @Test
    void generate_使用序列并格式化为BOX加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                eq("SELECT nextval('seq_archive_box_no')"), eq(Long.class)))
                .thenReturn(1L);

        BoxNoUtil util = new BoxNoUtil(jdbc);

        assertThat(util.generate()).isEqualTo("BOX-000001");
    }

    @Test
    void generate_大序号也能正确补零() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                eq("SELECT nextval('seq_archive_box_no')"), eq(Long.class)))
                .thenReturn(12345L);

        assertThat(new BoxNoUtil(jdbc).generate()).isEqualTo("BOX-012345");
    }
}
