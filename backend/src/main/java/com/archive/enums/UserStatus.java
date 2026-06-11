package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    active("active"),
    disabled("disabled");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}
