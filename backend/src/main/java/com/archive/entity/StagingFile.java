package com.archive.entity;

import com.archive.enums.MatchStatus;
import com.archive.enums.ScanResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 前台接收阶段暂存电子文件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "staging_files", autoResultMap = true)
public class StagingFile extends BaseEntity {

    private Long batchId;

    private Long itemId;

    private String uploadBatchNo;

    private String originalFilename;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private String sha256;

    private String bucketName;

    private String objectKey;

    @EnumValue
    private MatchStatus matchStatus;

    @EnumValue
    private ScanResult scanResult;

    private String scanMessage;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> checkSummary;

    private Long uploadedBy;

    private OffsetDateTime uploadedAt;

    private Long archivedFileId;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
