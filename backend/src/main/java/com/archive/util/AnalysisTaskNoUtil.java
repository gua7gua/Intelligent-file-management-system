package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 研判任务号生成工具。调用 seq_analysis_task_no，格式 ANA-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class AnalysisTaskNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_analysis_task_no')", Long.class);
        return String.format("ANA-%06d", seq);
    }
}
