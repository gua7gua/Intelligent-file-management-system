package com.archive.entity;

import com.archive.enums.*;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 正式电子文件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("archive_files")
public class ArchiveFile extends BaseEntity {

    private Long archiveId;

    @EnumValue
    private FileRole fileRole;

    private String bucketName;

    private String objectKey;

    private String originalFilename;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private String sha256;

    @EnumValue
    private ScanResult scanResult;

    @EnumValue
    private UsabilityResult usabilityResult;

    @EnumValue
    private FileStatus fileStatus;

    private OffsetDateTime deletedAt;

    private Long deletedBy;
}
