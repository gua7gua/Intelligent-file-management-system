package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class FondsDetailResponse {
    private Long id;
    private String fondsNo;
    private String fondsName;
    private Long organizationId;
    private String organizationName;
    private String description;
    private String status;
    private Long archiveCount;
    private Long boxCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
