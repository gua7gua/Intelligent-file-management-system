package com.archive.enums;

import lombok.Getter;

/** 编研成果状态。name() 与 compilations.status CHECK 约束一一对应。 */
@Getter
public enum CompilationStatus {
    draft("草稿"),
    generated("已生成"),
    archived("已入库");

    private final String displayName;

    CompilationStatus(String displayName) {
        this.displayName = displayName;
    }
}
