package com.archive.enums;

import lombok.Getter;

/**
 * 来源类型：移交、征集、编研。
 */
@Getter
public enum SourceType {

    transfer("移交"),
    collection("征集"),
    compilation("编研");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }
}
