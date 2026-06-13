package com.archive.enums;

import lombok.Getter;

/**
 * 档案实体状态枚举。
 */
@Getter
public enum ConditionStatus {

    normal("正常"),
    damaged("损坏"),
    repairing("修复中"),
    lost("丢失"),
    destroyed("已销毁");

    private final String displayName;

    ConditionStatus(String displayName) {
        this.displayName = displayName;
    }
}
