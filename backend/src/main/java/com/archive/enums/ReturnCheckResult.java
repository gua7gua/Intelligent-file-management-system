package com.archive.enums;

import lombok.Getter;

/**
 * 借阅归还检查结果枚举。
 * name() 与 borrow_requests.return_check_result 的 CHECK 约束一一对应。
 */
@Getter
public enum ReturnCheckResult {

    normal("正常"),
    damaged("破损"),
    missing_page("缺页"),
    other("其他");

    private final String displayName;

    ReturnCheckResult(String displayName) {
        this.displayName = displayName;
    }
}
