package com.archive.enums;

import lombok.Getter;

/** 销毁清册状态。 */
@Getter
public enum DestructionListStatus {
    draft("草稿"),
    pending_approval("待审批"),
    pending_destroy("待销毁"),
    destroyed("已销毁");

    private final String displayName;

    DestructionListStatus(String displayName) {
        this.displayName = displayName;
    }
}
