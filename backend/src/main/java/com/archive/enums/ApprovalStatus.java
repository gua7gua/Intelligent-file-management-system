package com.archive.enums;

import lombok.Getter;

/**
 * 审批单状态枚举。
 */
@Getter
public enum ApprovalStatus {

    pending("待审批"),
    approved("已通过"),
    rejected("已退回");

    private final String displayName;

    ApprovalStatus(String displayName) {
        this.displayName = displayName;
    }
}
