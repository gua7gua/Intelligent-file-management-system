package com.archive.dto.request;

import lombok.Data;

@Data
public class ApprovalOpinionRequest {

    /** 审批通过可空；退回必填（服务层校验）。 */
    private String opinion;
}
