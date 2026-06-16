package com.archive.dto.response;

import com.archive.dto.request.AnalysisRule;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AnalysisTaskDetailResponse extends AnalysisTaskResponse {

    /** 规则快照（从 analysis_tasks.rule_snapshot 反序列化）。 */
    private AnalysisRule rule;

    private List<AnalysisItemResponse> items;
}
