package com.archive.enums;

import lombok.Getter;

/**
 * 暂存文件匹配状态。
 */
@Getter
public enum MatchStatus {

    unmatched("未匹配"),
    matched("已匹配"),
    duplicate("重复"),
    failed_check("检查失败"),
    archived("已归档"),
    deleted("已删除");

    private final String displayName;

    MatchStatus(String displayName) {
        this.displayName = displayName;
    }
}
