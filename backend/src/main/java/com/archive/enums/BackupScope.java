package com.archive.enums;

import lombok.Getter;

/** 备份范围。name() 与 backup_tasks.backup_scope CHECK 约束一一对应。 */
@Getter
public enum BackupScope {
    database("数据库"),
    files("文件"),
    both("全部");

    private final String displayName;

    BackupScope(String displayName) {
        this.displayName = displayName;
    }
}
