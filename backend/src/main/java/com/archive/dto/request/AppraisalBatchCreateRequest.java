package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppraisalBatchCreateRequest {

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
}
