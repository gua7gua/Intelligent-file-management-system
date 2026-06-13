package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 保管期限枚举。
 * 数据库存储值为 10y / 30y / permanent。
 */
@Getter
public enum RetentionPeriod {

    _10y("10y", "10年"),
    _30y("30y", "30年"),
    permanent("permanent", "永久");

    /** 数据库存储值。 */
    @EnumValue
    private final String dbValue;

    private final String displayName;

    RetentionPeriod(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }
}
