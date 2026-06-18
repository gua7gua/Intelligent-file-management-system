package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AppraisalBatchResponse {

    private Long id;
    private String batchNo;
    private String batchName;
    private Integer categoryId;
    private String categoryName;
    private Integer formedYearStart;
    private Integer formedYearEnd;
    /** 到期窗口天数（D4）。 */
    private Integer dueDays;
    private String status;
    private Integer hitCount;
    private Integer destroyCount;
    private Integer extendCount;
    private OffsetDateTime completedAt;
    private Long generatedListId;
    private String generatedListNo;
}
