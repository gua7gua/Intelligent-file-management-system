package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    @EnumValue active("active"),
    @EnumValue disabled("disabled");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}
