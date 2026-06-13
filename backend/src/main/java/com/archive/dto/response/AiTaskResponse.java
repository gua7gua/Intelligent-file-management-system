package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AiTaskResponse {
    private Long aiTaskId;
    private String taskNo;
    private String status;
    private Integer totalBatches;
    private Integer successBatches;
    private Integer failedBatches;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
