package com.archive.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppraisalBatchCreateRequest {

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    private Integer categoryId;

    /**
     * 到期窗口天数（D4）：命中保管期限到期日 retention_until <= 今天 + dueDays 的档案，
     * 含今天前已过期未处理的档案。0 表示只圈已过期。
     */
    @NotNull(message = "到期窗口天数不能为空")
    @Min(value = 0, message = "到期窗口天数不能为负")
    private Integer dueDays;
}
