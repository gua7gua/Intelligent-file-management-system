package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserType {
    @EnumValue internal("internal"),
    @EnumValue public_("public");

    @EnumValue
    private final String value;

    UserType(String value) {
        this.value = value;
    }
}
