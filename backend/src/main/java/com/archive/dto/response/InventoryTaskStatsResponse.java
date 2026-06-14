package com.archive.dto.response;

import lombok.Data;

/**
 * 盘点任务统计（按核对结果分组计数）。
 */
@Data
public class InventoryTaskStatsResponse {

    private long normal = 0L;
    private long missing = 0L;
    private long misplaced = 0L;
    private long damaged = 0L;
    private long onLoan = 0L;
}
