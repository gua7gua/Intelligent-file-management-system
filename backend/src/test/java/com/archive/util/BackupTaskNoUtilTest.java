package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BackupTaskNoUtilTest {

    @Test
    void generate_格式与序列一致() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_backup_task_no')"), eq(Long.class)))
                .thenReturn(25L);
        assertThat(new BackupTaskNoUtil(jdbc).generate()).isEqualTo("BAK-000025");
    }
}
