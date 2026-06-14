package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 核验凭证并确认出库入参（12.4）。dueAt 须晚于当前时间，由 Service 层校验。
 */
@Data
public class BorrowCheckoutRequest {

    @NotBlank(message = "凭证号不能为空")
    private String voucherNo;

    @NotNull(message = "应还时间不能为空")
    private OffsetDateTime dueAt;

    @Size(max = 500)
    private String note;
}
