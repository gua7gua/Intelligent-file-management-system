package com.archive.enums;

import lombok.Getter;

/**
 * 审批类型枚举。
 */
@Getter
public enum ApprovalType {

    security_adjust("密级调整"),
    open_adjust("开放调整"),
    destruction("销毁审批");

    private final String displayName;

    ApprovalType(String displayName) {
        this.displayName = displayName;
    }
}
