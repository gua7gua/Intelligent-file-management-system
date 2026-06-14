package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 销毁清册号生成工具。调用 seq_destruction_list_no，格式 DES-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class DestructionNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_destruction_list_no')", Long.class);
        return String.format("DES-%06d", seq);
    }
}
