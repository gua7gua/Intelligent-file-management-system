package com.archive.entity;

import com.archive.enums.InventoryCheckResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 盘点明细实体（inventory_items）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_items")
public class InventoryItem extends BaseEntity {

    private Long taskId;

    private Long archiveId;

    private Long boxId;

    private Long expectedLocationId;

    private String actualLocationCode;

    @EnumValue
    private InventoryCheckResult checkResult;

    private String note;

    private OffsetDateTime deletedAt;
}
