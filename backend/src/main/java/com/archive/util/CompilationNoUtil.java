package com.archive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 编研成果号生成工具。调用 seq_compilation_no，格式 CMP-{6位序号}。 */
@Component
@RequiredArgsConstructor
public class CompilationNoUtil {

    private final JdbcTemplate jdbcTemplate;

    public String generate() {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT nextval('seq_compilation_no')", Long.class);
        return String.format("CMP-%06d", seq);
    }
}
