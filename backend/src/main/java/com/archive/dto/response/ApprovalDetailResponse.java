package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalDetailResponse {

    private Long id;
    private String approvalType;
    private String targetType;
    private Long targetId;
    private Long evidenceArchiveId;
    private String evidenceArchiveNo;
    private String oldValue;
    private String newValue;
    private String reason;
    private String status;
    private Long submittedBy;
    private OffsetDateTime submittedAt;
    private Long approvedBy;
    private OffsetDateTime approvedAt;
    private String approvalOpinion;
    private String targetSummary;
}
