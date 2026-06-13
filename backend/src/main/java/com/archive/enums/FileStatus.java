package com.archive.enums;

import lombok.Getter;

/**
 * 档案文件状态枚举。
 */
@Getter
public enum FileStatus {

    pending("待处理"),
    normal("正常"),
    failed("失败"),
    deleted("已删除");

    private final String displayName;

    FileStatus(String displayName) {
        this.displayName = displayName;
    }
}
