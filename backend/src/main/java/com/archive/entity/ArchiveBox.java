package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 档案盒。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("archive_boxes")
public class ArchiveBox extends BaseEntity {

    private String boxNo;

    private Long locationId;

    private Integer categoryId;

    private Long fondsId;

    private String yearLabel;

    private String spineText;

    private Integer capacity;

    private Integer usedCount;

    private String status;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
