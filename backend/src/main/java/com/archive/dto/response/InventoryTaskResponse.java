package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InventoryTaskResponse {

    private Long id;
    private String taskNo;
    private String taskName;
    private Long roomId;
    private String roomNo;
    private Integer categoryId;
    private String categoryName;
    private String status;
    private Long total;
    private Long checked;
    private Long abnormalCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String summary;
    private OffsetDateTime createdAt;
}
