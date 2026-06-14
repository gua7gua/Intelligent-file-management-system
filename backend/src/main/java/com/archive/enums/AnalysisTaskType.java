package com.archive.enums;

import lombok.Getter;

/** 研判任务类型。name() 与 analysis_tasks.task_type CHECK 约束一一对应。 */
@Getter
public enum AnalysisTaskType {
    missing_fields("缺字段"),
    category_conflict("门类冲突"),
    mixed("综合");

    private final String displayName;

    AnalysisTaskType(String displayName) {
        this.displayName = displayName;
    }
}
