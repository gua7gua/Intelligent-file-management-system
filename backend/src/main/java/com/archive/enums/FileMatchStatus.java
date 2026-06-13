package com.archive.enums;

import lombok.Getter;

/**
 * 清单条目级电子文件匹配汇总状态。
 */
@Getter
public enum FileMatchStatus {

    none("无匹配"),
    matched("已匹配"),
    missing("缺失"),
    duplicate("重复"),
    failed("检查失败");

    private final String displayName;

    FileMatchStatus(String displayName) {
        this.displayName = displayName;
    }
}
