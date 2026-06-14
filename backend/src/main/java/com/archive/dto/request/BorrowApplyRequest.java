package com.archive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 提交借阅申请入参（11.7）。
 */
@Data
public class BorrowApplyRequest {

    @NotNull(message = "档案 ID 不能为空")
    private Long archiveId;

    @NotBlank(message = "借阅理由不能为空")
    @Size(max = 500, message = "借阅理由不超过 500 字")
    private String reason;

    @NotNull(message = "预计借阅天数不能为空")
    @Min(value = 1, message = "预计借阅天数至少 1 天")
    @Max(value = 365, message = "预计借阅天数不超过 365 天")
    private Integer expectedDays;

    /** 预计到馆取件时间，可空。 */
    private OffsetDateTime expectedVisitAt;

    /** 联系电话，可空；非空时须为 11 位手机号。 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
}
