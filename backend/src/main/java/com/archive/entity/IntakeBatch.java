package com.archive.entity;

import com.archive.enums.BatchStatus;
import com.archive.enums.SourceType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 移交/征集清单批次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intake_batches")
public class IntakeBatch extends BaseEntity {

    private String batchNo;

    @EnumValue
    private SourceType sourceType;

    private String title;

    @EnumValue
    private BatchStatus status;

    private Long organizationId;

    private String departmentName;

    private Long publicUserId;

    private String contactName;

    private String contactPhone;

    private Integer archiveYear;

    private LocalDate expectedTransferDate;

    private OffsetDateTime scheduledReceiveAt;

    private OffsetDateTime submittedAt;

    private Long acceptedBy;

    private OffsetDateTime acceptedAt;

    private OffsetDateTime archivedAt;

    private OffsetDateTime shelvedAt;

    private Long latestAiTaskId;

    private String rejectReason;

    private OffsetDateTime agreementAcceptedAt;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
