package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/** 销毁明细（快照）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("destruction_items")
public class DestructionItem extends BaseEntity {

    private Long destructionListId;
    private Long archiveId;
    private String archiveNoSnapshot;
    private String titleSnapshot;
    private String categorySnapshot;
    private Integer pageCountSnapshot;
    private String retentionSnapshot;
    private Integer securityLevelSnapshot;
    private String appraisalOpinionSnapshot;

    /** not_started / deleted / failed。 */
    private String fileDeleteStatus;
    private OffsetDateTime fileDeletedAt;
}
