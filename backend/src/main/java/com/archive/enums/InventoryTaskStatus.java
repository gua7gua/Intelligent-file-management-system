package com.archive.enums;

import lombok.Getter;

/** 盘点任务状态。name() 与 inventory_tasks.status CHECK 约束一一对应。 */
@Getter
public enum InventoryTaskStatus {
    draft("草稿"),
    running("盘点中"),
    completed("已完成");

    private final String displayName;

    InventoryTaskStatus(String displayName) {
        this.displayName = displayName;
    }
}
