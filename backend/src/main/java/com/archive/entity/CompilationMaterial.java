package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 编研素材引用实体（compilation_materials）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compilation_materials")
public class CompilationMaterial extends BaseEntity {

    private Long compilationId;
    private Long archiveId;
    private Integer sortNo;
    private String quoteNote;

    private OffsetDateTime deletedAt;
}
