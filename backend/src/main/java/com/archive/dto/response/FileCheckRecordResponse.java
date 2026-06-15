package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FileCheckRecordResponse {

    private Long id;
    private String targetType;
    private Long targetId;
    private String checkType;
    private String checkResult;
    private String expectedHash;
    private String actualHash;
    private String signatureResult;
    private String message;
    private OffsetDateTime checkedAt;
}
