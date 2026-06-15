package com.archive.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AnalysisTaskDetailResponse extends AnalysisTaskResponse {

    private List<AnalysisItemResponse> items;
}
