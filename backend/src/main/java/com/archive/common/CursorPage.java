package com.archive.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CursorPage {
    @Min(value = 1, message = "limit 最小为 1")
    @Max(value = 100, message = "limit 最大为 100")
    private Integer limit = 20;

    /** 上一页返回的 nextCursor；首页为空 */
    private String cursor;
}
