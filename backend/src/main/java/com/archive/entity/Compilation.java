package com.archive.entity;

import com.archive.enums.CompilationStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 编研成果实体（compilations）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compilations")
public class Compilation extends BaseEntity {

    private String compilationNo;
    private String title;
    private String compilationType;
    private String dateRangeText;
    private String keywords;
    private String summary;
    private String contentHtml;

    @EnumValue
    private CompilationStatus status;

    private Long generatedFileAttachmentId;
    private Long generatedArchiveFileId;
    private Long generatedArchiveId;

    private OffsetDateTime deletedAt;
}
