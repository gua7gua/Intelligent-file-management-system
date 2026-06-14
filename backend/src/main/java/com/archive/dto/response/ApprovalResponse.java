package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalResponse {

    private Long id;
    private String approvalType;
    private String targetType;
    private Long targetId;
    private String status;
    private String reason;
    private Long submittedBy;
    private OffsetDateTime submittedAt;
    private String approvalOpinion;
    /** 目标摘要（档号/清册号）。 */
    private String targetSummary;
}
