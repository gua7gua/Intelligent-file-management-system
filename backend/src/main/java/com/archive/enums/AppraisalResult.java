package com.archive.enums;

import lombok.Getter;

/** 鉴定结论。 */
@Getter
public enum AppraisalResult {
    extend("延长保管"),
    destroy("销毁");

    private final String displayName;

    AppraisalResult(String displayName) {
        this.displayName = displayName;
    }
}
