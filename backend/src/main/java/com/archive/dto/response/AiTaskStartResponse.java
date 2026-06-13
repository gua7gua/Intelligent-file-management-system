package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiTaskStartResponse {
    private Long aiTaskId;
    private String taskNo;
    private String status;
    private Integer batchSize;
    private Integer totalBatches;
}
