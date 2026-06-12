package com.archive.enums;

import lombok.Getter;

/**
 * 清单条目状态枚举。
 */
@Getter
public enum ItemStatus {

    draft("草稿"),
    pending_acceptance("待验收"),
    accepted("已接收"),
    rejected("已回退"),
    pending_archive("待入库"),
    archived("已入库");

    private final String displayName;

    ItemStatus(String displayName) {
        this.displayName = displayName;
    }
}
