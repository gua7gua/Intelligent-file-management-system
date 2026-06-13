package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 机架-层-盒位架位。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("storage_locations")
public class StorageLocation extends BaseEntity {

    private Long roomId;

    private Integer rackNo;

    private Integer layerNo;

    private Integer boxSlotNo;

    private String locationCode;

    private String status;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
