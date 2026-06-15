package com.archive.enums;

import lombok.Getter;

/** 研判异常项处理状态。name() 与 analysis_items.status CHECK 约束一一对应。 */
@Getter
public enum AnalysisItemStatus {
    pending("待处理"),
    adopted("已采纳"),
    rejected("已驳回");

    private final String displayName;

    AnalysisItemStatus(String displayName) {
        this.displayName = displayName;
    }
}
