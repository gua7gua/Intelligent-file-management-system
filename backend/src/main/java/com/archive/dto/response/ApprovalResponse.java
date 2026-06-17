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
    private String submittedByName;
    private OffsetDateTime submittedAt;
    private String approvalOpinion;
    /** 目标摘要（档号/清册号）。 */
    private String targetSummary;
    /** 目标档案题名（archive 类型审批使用）。 */
    private String targetArchiveNo;
    private String targetArchiveTitle;
    /** 目标销毁清册名（destruction 类型审批使用）。 */
    private String targetListNo;
    private String targetListName;
}
