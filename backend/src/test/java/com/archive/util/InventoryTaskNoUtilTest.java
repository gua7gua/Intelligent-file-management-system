package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InventoryTaskNoUtilTest {

    @Test
    void generate_格式与序列一致() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_inventory_task_no')"), eq(Long.class)))
                .thenReturn(7L);
        assertThat(new InventoryTaskNoUtil(jdbc).generate()).isEqualTo("INV-000007");
    }
}
