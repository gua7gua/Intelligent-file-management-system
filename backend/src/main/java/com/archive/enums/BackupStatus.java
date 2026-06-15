package com.archive.enums;

import lombok.Getter;

/** 备份任务状态。name() 与 backup_tasks.status CHECK 约束一一对应。 */
@Getter
public enum BackupStatus {
    running("进行中"),
    success("成功"),
    failed("失败");

    private final String displayName;

    BackupStatus(String displayName) {
        this.displayName = displayName;
    }
}
