package com.archive.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新库房请求（仅名称/阈值/状态可改）。
 */
@Data
public class WarehouseRoomUpdateRequest {

    private String roomName;

    private BigDecimal warningThreshold;

    /** active / disabled。 */
    private String status;
}
