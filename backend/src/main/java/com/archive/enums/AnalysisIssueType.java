package com.archive.enums;

import lombok.Getter;

/** 研判异常项类型。name() 与 analysis_items.issue_type CHECK 约束一一对应。 */
@Getter
public enum AnalysisIssueType {
    missing_field("缺字段"),
    category_conflict("门类冲突"),
    tag_suggestion("标签建议"),
    tag_wrong("标签错配");

    private final String displayName;

    AnalysisIssueType(String displayName) {
        this.displayName = displayName;
    }
}
