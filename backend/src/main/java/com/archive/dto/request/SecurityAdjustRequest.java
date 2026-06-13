package com.archive.dto.request;

import lombok.Data;

/**
 * 发起密级调整审批（10.4）。
 */
@Data
public class SecurityAdjustRequest {

    private Integer newSecurityLevel;

    private String evidenceArchiveNo;

    private String reason;
}
