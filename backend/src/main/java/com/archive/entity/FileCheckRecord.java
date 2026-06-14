package com.archive.entity;

import com.archive.enums.FileCheckResult;
import com.archive.enums.FileCheckTargetType;
import com.archive.enums.FileCheckType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 四性检测记录实体（file_check_records）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_check_records")
public class FileCheckRecord extends BaseEntity {

    @EnumValue
    private FileCheckTargetType targetType;

    private Long targetId;

    @EnumValue
    private FileCheckType checkType;

    @EnumValue
    private FileCheckResult checkResult;

    private String expectedHash;

    private String actualHash;

    private String signatureObjectKey;

    private String signatureAlgorithm;

    /** null / passed / failed / not_configured，存字符串。 */
    private String signatureResult;

    private OffsetDateTime signatureCheckedAt;

    private String message;

    private OffsetDateTime checkedAt;

    private OffsetDateTime deletedAt;
}
