package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 档案字段变更日志。
 */
@Data
@TableName("archive_change_logs")
public class ArchiveChangeLog {

    private Long id;

    private Long archiveId;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private String changeReason;

    private String changeSource;

    private Long approvalRequestId;

    private Long changedBy;

    private OffsetDateTime changedAt;
}
