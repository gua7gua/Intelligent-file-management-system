package com.archive.enums;

import lombok.Getter;

/**
 * 载体状态枚举。
 */
@Getter
public enum CarrierStatus {

    electronic("纯电子"),
    paper_electronic("纸质+电子"),
    paper("纯纸质");

    private final String displayName;

    CarrierStatus(String displayName) {
        this.displayName = displayName;
    }
}
