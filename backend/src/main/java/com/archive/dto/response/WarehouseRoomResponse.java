package com.archive.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库房列表项，含占用率与告警状态。
 */
@Data
public class WarehouseRoomResponse {

    private Long id;
    private String roomNo;
    private String roomName;
    private Integer rackCount;
    private Integer layersPerRack;
    private Integer boxesPerLayer;
    private Integer capacity;
    private BigDecimal warningThreshold;
    private String status;

    /** 已占用盒位数。 */
    private Integer occupiedSlots;
    /** 占用率，0~1。 */
    private BigDecimal occupancyRate;
    /** 是否达到告警阈值。 */
    private Boolean warning;
}
