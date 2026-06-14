package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AnalysisTaskResponse {

    private Long id;
    private String taskNo;
    private String taskType;
    private String status;
    private Long latestAiTaskId;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
