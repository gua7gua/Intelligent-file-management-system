package com.archive.enums;

import lombok.Getter;

/** 四性检测结果。name() 与 file_check_records.check_result CHECK 约束一一对应。 */
@Getter
public enum FileCheckResult {
    passed("通过"),
    failed("未通过"),
    not_configured("未配置");

    private final String displayName;

    FileCheckResult(String displayName) {
        this.displayName = displayName;
    }
}
