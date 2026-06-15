package com.archive.enums;

import lombok.Getter;

/** 研判任务状态。name() 与 analysis_tasks.status CHECK 约束一一对应。 */
@Getter
public enum AnalysisTaskStatus {
    running("进行中"),
    partial_completed("部分完成"),
    completed("已完成"),
    failed("失败");

    private final String displayName;

    AnalysisTaskStatus(String displayName) {
        this.displayName = displayName;
    }
}
