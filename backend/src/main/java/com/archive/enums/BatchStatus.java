package com.archive.enums;

import lombok.Getter;

/**
 * 清单批次状态枚举。
 */
@Getter
public enum BatchStatus {

    draft("草稿"),
    pending_transfer("待移交"),
    pending_contact("待联系"),
    pending_receive("待接收"),
    received("已接收"),
    partially_received("部分接收"),
    rejected("已回退"),
    archived("已入库"),
    shelved("已上架");

    private final String displayName;

    BatchStatus(String displayName) {
        this.displayName = displayName;
    }
}
