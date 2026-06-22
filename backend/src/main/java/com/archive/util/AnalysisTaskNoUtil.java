package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 研判任务号生成工具。调用 seq_analysis_task_no，格式 ANA-YYYYMM-NNNN（如 ANA-202606-0001）。 */
@Component
@RequiredArgsConstructor
public class AnalysisTaskNoUtil {

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_analysis_task_no')", Long.class);
        String ym = LocalDate.now().format(YYYYMM);
        return String.format("ANA-%s-%04d", ym, seq);
    }
}
