package com.archive.entity;

import com.archive.enums.InventoryTaskStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 盘点任务实体（inventory_tasks）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_tasks")
public class InventoryTask extends BaseEntity {

    private String taskNo;

    private String taskName;

    private Long roomId;

    private Integer categoryId;

    @EnumValue
    private InventoryTaskStatus status;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    private String summary;

    /** 软删除标记，项目未启用 @TableLogic，查询时手动 isNull("deleted_at")。 */
    private OffsetDateTime deletedAt;
}
