package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 待入库批次列表项（9.1）。
 */
@Data
public class PendingBatchResponse {

    private Long id;

    private String batchNo;

    private String sourceType;

    private String title;

    private String status;

    private String organizationName;

    private String contactName;

    private OffsetDateTime acceptedAt;

    /** 已接收条目总数。 */
    private int itemCount;

    /** 已入库条目数。 */
    private int archivedCount;

    /** 待入库条目数。 */
    private int pendingArchiveCount;

    /** 最近 AI 任务状态。 */
    private String latestAiTaskStatus;
}
