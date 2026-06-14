package com.archive.enums;

import lombok.Getter;

/** 销毁方式。 */
@Getter
public enum DestroyMethod {
    shredding("粉碎"),
    burning("焚毁"),
    entrusted("委托销毁");

    private final String displayName;

    DestroyMethod(String displayName) {
        this.displayName = displayName;
    }
}
