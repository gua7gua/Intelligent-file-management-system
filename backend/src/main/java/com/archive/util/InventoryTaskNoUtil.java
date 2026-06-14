package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 盘点任务号生成工具。调用 seq_inventory_task_no，格式 INV-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class InventoryTaskNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_inventory_task_no')", Long.class);
        return String.format("INV-%06d", seq);
    }
}
