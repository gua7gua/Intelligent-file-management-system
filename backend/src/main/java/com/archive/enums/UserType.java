package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum UserType {
    internal("internal"),
    public_("public");

    @EnumValue
    private final String value;

    UserType(String value) {
        this.value = value;
    }
}
