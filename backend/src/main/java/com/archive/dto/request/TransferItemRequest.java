package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 移交清单条目请求（新增/编辑共用）。
 */
@Data
public class TransferItemRequest {

    @NotBlank(message = "档案标题不能为空")
    private String inputTitle;

    private Integer pageCount;

    @NotBlank(message = "保管期限不能为空")
    private String retentionPeriod;

    @NotBlank(message = "载体状态不能为空")
    private String carrierStatus;

    @NotNull(message = "密级不能为空")
    private Integer securityLevel;

    @NotBlank(message = "公开状态不能为空")
    private String openStatus;

    private Boolean allowDigitization;

    private String electronicFormat;

    private String expectedFilename;

    private LocalDate formedDate;
}
