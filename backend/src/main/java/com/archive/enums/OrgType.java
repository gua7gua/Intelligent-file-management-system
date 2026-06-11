package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrgType {
    archive_org("archive_org"),
    government("government"),
    enterprise("enterprise"),
    public_institution("public_institution");

    @EnumValue
    private final String value;

    OrgType(String value) {
        this.value = value;
    }
}
