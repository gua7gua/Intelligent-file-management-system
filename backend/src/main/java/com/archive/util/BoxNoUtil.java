package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 档案盒号生成工具。
 * 使用 seq_archive_box_no 序列生成 BOX-{6位序号} 格式盒号。
 * 唯一生成点：新增档案盒时。
 */
@Component
@RequiredArgsConstructor
public class BoxNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_archive_box_no')", Long.class);
        return String.format("BOX-%06d", seq);
    }
}
