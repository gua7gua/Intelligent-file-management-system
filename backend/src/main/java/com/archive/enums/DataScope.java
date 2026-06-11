package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DataScope {
    @EnumValue own_org("own_org"),
    @EnumValue own_fonds("own_fonds"),
    @EnumValue all("all");

    @EnumValue
    private final String value;

    DataScope(String value) {
        this.value = value;
    }
}
