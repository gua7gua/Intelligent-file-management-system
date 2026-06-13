package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 档号生成工具。
 * 使用 seq_archive_no 序列生成 ARC-{6位序号} 格式档号。
 * 唯一生成点：确认入库时。
 */
@Component
@RequiredArgsConstructor
public class ArchiveNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_archive_no')", Long.class);
        return String.format("ARC-%06d", seq);
    }
}
