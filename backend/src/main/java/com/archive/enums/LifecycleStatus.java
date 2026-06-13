package com.archive.enums;

import lombok.Getter;

/**
 * 正式档案生命周期状态枚举。
 */
@Getter
public enum LifecycleStatus {

    pending_shelf("待上架"),
    normal("正常"),
    pending_destruction("待销毁"),
    destroyed("已销毁");

    private final String displayName;

    LifecycleStatus(String displayName) {
        this.displayName = displayName;
    }
}
