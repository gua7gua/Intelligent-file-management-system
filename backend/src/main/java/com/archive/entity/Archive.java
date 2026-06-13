package com.archive.entity;

import com.archive.enums.*;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 正式档案主表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("archives")
public class Archive extends BaseEntity {

    private String archiveNo;

    private String title;

    private String responsibleText;

    private LocalDate formedDate;

    private Integer formedYear;

    private Integer categoryId;

    @EnumValue
    private SourceType sourceType;

    private Long sourceBatchId;

    private Long sourceItemId;

    private Long sourceCompilationId;

    private Long organizationId;

    private Long fondsId;

    @EnumValue
    private CarrierStatus carrierStatus;

    @EnumValue
    private RetentionPeriod retentionPeriod;

    private LocalDate retentionUntil;

    private Integer securityLevel;

    private String openStatus;

    private Boolean allowDigitization;

    @EnumValue
    private LifecycleStatus lifecycleStatus;

    @EnumValue
    private LoanStatus loanStatus;

    @EnumValue
    private ConditionStatus conditionStatus;

    private OffsetDateTime archivedAt;

    private OffsetDateTime shelvedAt;
}
