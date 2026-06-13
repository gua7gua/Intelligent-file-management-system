package com.archive.enums;

import lombok.Getter;

/**
 * 档案文件角色枚举。
 */
@Getter
public enum FileRole {

    original("原件"),
    scan("扫描件"),
    compilation_body("编研正文"),
    signature("签名件");

    private final String displayName;

    FileRole(String displayName) {
        this.displayName = displayName;
    }
}
