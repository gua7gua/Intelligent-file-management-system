package com.archive.enums;

import lombok.Getter;

/** 盘点明细核对结果。name() 与 inventory_items.check_result CHECK 约束一一对应。 */
@Getter
public enum InventoryCheckResult {
    normal("正常"),
    missing("缺失"),
    misplaced("错位"),
    damaged("受损"),
    on_loan("借出中");

    private final String displayName;

    InventoryCheckResult(String displayName) {
        this.displayName = displayName;
    }
}
