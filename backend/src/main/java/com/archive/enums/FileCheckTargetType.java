package com.archive.enums;

import lombok.Getter;

/** 四性检测目标类型。name() 与 file_check_records.target_type CHECK 约束一一对应。 */
@Getter
public enum FileCheckTargetType {
    staging_file("暂存文件"),
    archive_file("正式文件");

    private final String displayName;

    FileCheckTargetType(String displayName) {
        this.displayName = displayName;
    }
}
