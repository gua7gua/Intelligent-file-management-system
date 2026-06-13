package com.archive.dto.response;

import lombok.Data;

/**
 * 档案文件信息（10.6/10.7/10.8）。
 */
@Data
public class ArchiveFileResponse {

    private Long id;

    private Long archiveId;

    private String fileRole;

    private String originalFilename;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private String scanResult;

    private String usabilityResult;

    private String fileStatus;
}
