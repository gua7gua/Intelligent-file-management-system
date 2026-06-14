package com.archive.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批借阅申请入参（12.3）。
 * 拒绝时 opinion 必填，由 Service 层校验（Bean Validation 无法表达条件必填）。
 */
@Data
public class BorrowApproveRequest {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Size(max = 500)
    private String opinion;
}
