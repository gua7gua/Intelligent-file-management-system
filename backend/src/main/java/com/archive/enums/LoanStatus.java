package com.archive.enums;

import lombok.Getter;

/**
 * 档案借阅状态枚举。
 */
@Getter
public enum LoanStatus {

    available("可借"),
    on_loan("借出中"),
    not_on_shelf("未上架");

    private final String displayName;

    LoanStatus(String displayName) {
        this.displayName = displayName;
    }
}
