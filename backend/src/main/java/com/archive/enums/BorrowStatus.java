package com.archive.enums;

import lombok.Getter;

/**
 * 借阅申请状态枚举。
 * name() 与 borrow_requests.status 的 CHECK 约束一一对应，由全局 MybatisEnumTypeHandler 持久化。
 */
@Getter
public enum BorrowStatus {

    applied("已申请"),
    rejected("已拒绝"),
    approved("已审批通过"),
    voucher_issued("凭证已发出"),
    checked_out("已出库"),
    returned("已归还"),
    abnormal_return("异常归还");

    private final String displayName;

    BorrowStatus(String displayName) {
        this.displayName = displayName;
    }
}
