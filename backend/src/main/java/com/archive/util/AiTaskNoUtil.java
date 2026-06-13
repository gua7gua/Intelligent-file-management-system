package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 任务号生成工具。调用 seq_ai_task_no 序列，格式 AIT-{6位序号}。
 * 与 ArchiveNoUtil 模式一致。
 */
@Component
@RequiredArgsConstructor
public class AiTaskNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_ai_task_no')", Long.class);
        return String.format("AIT-%06d", seq);
    }
}
