package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DestructionListResponse {

    private Long id;
    private String listNo;
    private String listName;
    private Long appraisalBatchId;
    private String appraisalBatchNo;
    private String status;
    private Integer itemCount;
    private OffsetDateTime destroyedAt;
}
