package com.archive.enums;

import lombok.Getter;

/**
 * 文件可用性检测结果枚举。
 */
@Getter
public enum UsabilityResult {

    pending("待检测"),
    passed("通过"),
    failed("未通过");

    private final String displayName;

    UsabilityResult(String displayName) {
        this.displayName = displayName;
    }
}
