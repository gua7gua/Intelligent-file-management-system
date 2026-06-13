package com.archive.enums;

import lombok.Getter;

/**
 * 保管期限枚举。
 */
@Getter
public enum RetentionPeriod {

    _10y("10年"),
    _30y("30年"),
    permanent("永久");

    private final String displayName;

    RetentionPeriod(String displayName) {
        this.displayName = displayName;
    }
}
