package com.archive.entity;

import com.archive.enums.AppraisalResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 鉴定明细。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appraisal_items")
public class AppraisalItem extends BaseEntity {

    private Long batchId;
    private Long archiveId;

    @EnumValue
    private AppraisalResult appraisalResult;

    private String newRetentionPeriod;
    private LocalDate newRetentionUntil;
    private String opinion;
    private Long appraisedBy;
    private OffsetDateTime appraisedAt;
}
