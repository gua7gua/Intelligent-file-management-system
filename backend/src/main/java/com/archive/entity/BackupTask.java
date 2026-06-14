package com.archive.entity;

import com.archive.enums.BackupScope;
import com.archive.enums.BackupStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 备份任务实体（backup_tasks）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_tasks")
public class BackupTask extends BaseEntity {

    private String taskNo;

    @EnumValue
    private BackupScope backupScope;

    @EnumValue
    private BackupStatus status;

    private String backupPath;

    private Long fileSize;

    private String sha256;

    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    private String message;

    private OffsetDateTime deletedAt;
}
