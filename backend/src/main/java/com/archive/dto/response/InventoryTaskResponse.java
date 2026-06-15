package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InventoryTaskResponse {

    private Long id;
    private String taskNo;
    private String taskName;
    private Long roomId;
    private Integer categoryId;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String summary;
}
