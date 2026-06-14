package com.archive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 内部工作台借阅摘要（11.1 的我的申请/当前借阅/逾期提示）。
 */
@Data
@AllArgsConstructor
public class BorrowSummaryItem {

    private String requestNo;
    private Long archiveId;
    private String archiveNo;
    private String title;
    private String status;
    private OffsetDateTime dueAt;
    private OffsetDateTime appliedAt;
}
