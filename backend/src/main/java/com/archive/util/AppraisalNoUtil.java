package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 鉴定批次号生成工具。调用 seq_appraisal_batch_no，格式 APP-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class AppraisalNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_appraisal_batch_no')", Long.class);
        return String.format("APP-%06d", seq);
    }
}
