package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 备份任务号生成工具。调用 seq_backup_task_no，格式 BAK-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class BackupTaskNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_backup_task_no')", Long.class);
        return String.format("BAK-%06d", seq);
    }
}
