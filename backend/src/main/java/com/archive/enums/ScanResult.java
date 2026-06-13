package com.archive.enums;

import lombok.Getter;

/**
 * 病毒扫描结果。
 */
@Getter
public enum ScanResult {

    pending("待扫描"),
    safe("安全"),
    infected("感染病毒"),
    failed("扫描失败");

    private final String displayName;

    ScanResult(String displayName) {
        this.displayName = displayName;
    }
}
