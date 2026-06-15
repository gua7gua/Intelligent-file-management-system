package com.archive.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ArchiveAccessLogResponse {
    private Long id;
    private Long userId;
    private String userType;
    private Long archiveId;
    private Long archiveFileId;
    private String accessType;
    private String ipAddress;
    private OffsetDateTime accessedAt;
}
