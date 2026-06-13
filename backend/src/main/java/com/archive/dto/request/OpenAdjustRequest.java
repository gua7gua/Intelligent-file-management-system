package com.archive.dto.request;

import lombok.Data;

/**
 * 发起开放调整审批（10.5）。
 */
@Data
public class OpenAdjustRequest {

    private String newOpenStatus;

    private String evidenceArchiveNo;

    private String reason;
}
