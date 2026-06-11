package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DataScope {
    own_org("own_org"),
    own_fonds("own_fonds"),
    all("all");

    @EnumValue
    private final String value;

    DataScope(String value) {
        this.value = value;
    }
}
