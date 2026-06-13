package com.archive.entity;

import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 审批单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_requests")
public class ApprovalRequest extends BaseEntity {

    @EnumValue
    private ApprovalType approvalType;

    private String targetType;

    private Long targetId;

    private Long evidenceArchiveId;

    private String oldValue;

    private String newValue;

    private String reason;

    @EnumValue
    private ApprovalStatus status;

    private Long submittedBy;

    private OffsetDateTime submittedAt;

    private Long approvedBy;

    private OffsetDateTime approvedAt;

    private String approvalOpinion;
}
