package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DestructionNoUtilTest {

    @Test
    void generate_使用序列生成DES加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_destruction_list_no')"), eq(Long.class)))
                .thenReturn(7L);

        assertThat(new DestructionNoUtil(jdbc).generate()).isEqualTo("DES-000007");
    }
}
