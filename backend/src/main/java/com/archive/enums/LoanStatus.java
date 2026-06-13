package com.archive.enums;

import lombok.Getter;

/**
 * 档案借阅状态枚举。
 */
@Getter
public enum LoanStatus {

    available("可借"),
    on_loan("借出中");

    private final String displayName;

    LoanStatus(String displayName) {
        this.displayName = displayName;
    }
}
