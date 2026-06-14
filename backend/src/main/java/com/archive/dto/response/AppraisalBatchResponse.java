package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AppraisalBatchResponse {

    private Long id;
    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    private String status;
    private Integer itemCount;
    private OffsetDateTime completedAt;
}
