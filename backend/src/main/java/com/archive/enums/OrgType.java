package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrgType {
    @EnumValue archive_org("archive_org"),
    @EnumValue government("government"),
    @EnumValue enterprise("enterprise"),
    @EnumValue public_institution("public_institution");

    @EnumValue
    private final String value;

    OrgType(String value) {
        this.value = value;
    }
}
