package com.archive.dto.request;

import com.archive.common.PageRequest;
import com.archive.enums.AnalysisTaskType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AnalysisTaskQuery extends PageRequest {

    private String status;
    private AnalysisTaskType taskType;
}
