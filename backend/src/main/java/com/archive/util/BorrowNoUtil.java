package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 借阅编号生成工具。
 * 申请号 BRW-{6位序号} 使用 seq_borrow_request_no；
 * 凭证号 VCH-{6位序号} 使用 seq_borrow_voucher_no（首次导出凭证时生成）。
 */
@Component
@RequiredArgsConstructor
public class BorrowNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String nextRequestNo() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_borrow_request_no')", Long.class);
        return String.format("BRW-%06d", seq);
    }

    public String nextVoucherNo() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_borrow_voucher_no')", Long.class);
        return String.format("VCH-%06d", seq);
    }
}
