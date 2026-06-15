package com.archive.enums;

import lombok.Getter;

/** 四性检测类型。name() 与 file_check_records.check_type CHECK 约束一一对应。 */
@Getter
public enum FileCheckType {
    integrity("完整性"),
    usability("可用性"),
    authenticity("真实性"),
    security("安全性");

    private final String displayName;

    FileCheckType(String displayName) {
        this.displayName = displayName;
    }
}
