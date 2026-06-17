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

    /** 按 dbValue（10y/30y/permanent）解析，供前端入参反序列化使用。枚举名带下划线(_10y)与前端传入值不一致，故不能用 valueOf。 */
    public static RetentionPeriod fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (RetentionPeriod r : values()) {
            if (r.dbValue.equals(value)) return r;
        }
        throw new IllegalArgumentException("未知的保管期限: " + value);
    }
}
