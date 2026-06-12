package com.archive.enums;

import lombok.Getter;

/**
 * 清单来源类型：移交或征集。
 */
@Getter
public enum SourceType {

    transfer("移交"),
    collection("征集");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }
}
