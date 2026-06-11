package com.archive.enums;

import lombok.Getter;

@Getter
public enum SecurityLevel {
    NONE(0, "非密"),
    INTERNAL(1, "内部"),
    SECRET(2, "秘密"),
    CONFIDENTIAL(3, "机密"),
    TOP_SECRET(4, "绝密");

    private final int level;
    private final String displayName;

    SecurityLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
}
