package com.archive.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 概览用库房告警项。
 */
@Data
public class WarehouseWarningResponse {

    private Long roomId;
    private String roomNo;
    private String roomName;
    private Integer occupiedSlots;
    private Integer capacity;
    private BigDecimal occupancyRate;
    private BigDecimal warningThreshold;
}
