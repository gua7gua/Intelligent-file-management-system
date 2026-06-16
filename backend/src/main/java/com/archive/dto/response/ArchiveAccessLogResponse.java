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
    /** 档号（联表 archives 填充，公众概览展示用）。 */
    private String archiveNo;
    /** 档案题名（联表 archives 填充）。 */
    private String title;
}
