package com.archive.dto.response;

import lombok.Data;

/**
 * 单个暂存文件详情。
 */
@Data
public class StagingFileResponse {

    private Long fileId;
    private String originalFilename;
    private Long fileSize;
    private String sha256;
    private String scanResult;
    private String scanMessage;
    private String matchStatus;
    private Long matchedItemId;
}
