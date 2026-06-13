package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 库房房间。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse_rooms")
public class WarehouseRoom extends BaseEntity {

    private String roomNo;

    private String roomName;

    private Integer rackCount;

    private Integer layersPerRack;

    private Integer boxesPerLayer;

    private Integer capacity;

    private BigDecimal warningThreshold;

    private String status;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
