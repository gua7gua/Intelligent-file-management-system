package com.archive.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowNoUtilTest {

    @Test
    void 申请号格式为BRW加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_borrow_request_no')"), eq(Long.class)))
                .thenReturn(1L);

        assertThat(new BorrowNoUtil(jdbc).nextRequestNo()).isEqualTo("BRW-000001");
    }

    @Test
    void 申请号大序号也能补零() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_borrow_request_no')"), eq(Long.class)))
                .thenReturn(123456L);

        assertThat(new BorrowNoUtil(jdbc).nextRequestNo()).isEqualTo("BRW-123456");
    }

    @Test
    void 凭证号格式为VCH加六位序号() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT nextval('seq_borrow_voucher_no')"), eq(Long.class)))
                .thenReturn(7L);

        assertThat(new BorrowNoUtil(jdbc).nextVoucherNo()).isEqualTo("VCH-000007");
    }
}
