package com.archive.dto.request;

import com.archive.enums.AnalysisTaskType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalysisTaskCreateRequest {

    @NotNull(message = "研判类型不能为空")
    private AnalysisTaskType taskType;

    @Valid
    private AnalysisRule rule;
}
