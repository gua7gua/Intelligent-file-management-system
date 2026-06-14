package com.archive.enums;

import lombok.Getter;

/** 鉴定批次状态。 */
@Getter
public enum AppraisalBatchStatus {
    draft("草稿"),
    completed("已完成");

    private final String displayName;

    AppraisalBatchStatus(String displayName) {
        this.displayName = displayName;
    }
}
