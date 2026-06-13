package com.archive.dto.response;

import lombok.Data;

/**
 * 确认入库结果（9.7）。
 */
@Data
public class ArchiveResultResponse {

    private Long archiveId;

    private String archiveNo;

    private String lifecycleStatus;
}
