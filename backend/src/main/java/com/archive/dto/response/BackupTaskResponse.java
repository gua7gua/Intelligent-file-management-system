package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BackupTaskResponse {

    private Long id;
    private String taskNo;
    private String backupScope;
    private String status;
    private String backupPath;
    private Long fileSize;
    private String sha256;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String message;
}
