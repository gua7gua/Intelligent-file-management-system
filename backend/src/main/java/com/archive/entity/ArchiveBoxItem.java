package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 盒内档案关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("archive_box_items")
public class ArchiveBoxItem extends BaseEntity {

    private Long boxId;

    private Long archiveId;

    private Integer sortNo;

    private Integer pageCount;

    private String physicalStatus;
}
