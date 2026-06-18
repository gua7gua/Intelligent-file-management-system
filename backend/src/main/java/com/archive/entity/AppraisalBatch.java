package com.archive.entity;

import com.archive.enums.AppraisalBatchStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/** 鉴定批次。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appraisal_batches")
public class AppraisalBatch extends BaseEntity {

    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    /** 到期窗口天数（D4）：命中 retention_until <= 今天 + due_days，含已过期未处理档案。 */
    private Integer dueDays;

    @EnumValue
    private AppraisalBatchStatus status;

    private OffsetDateTime completedAt;
}
