package com.archive.enums;

import lombok.Getter;

/**
 * 档案公开状态枚举。
 */
@Getter
public enum OpenStatus {

    open("公开"),
    closed("不公开");

    private final String displayName;

    OpenStatus(String displayName) {
        this.displayName = displayName;
    }
}
