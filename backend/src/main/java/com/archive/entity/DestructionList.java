package com.archive.entity;

import com.archive.enums.DestroyMethod;
import com.archive.enums.DestructionListStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/** 销毁清册。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("destruction_lists")
public class DestructionList extends BaseEntity {

    private String listNo;
    private String listName;
    private Long appraisalBatchId;

    @EnumValue
    private DestructionListStatus status;

    private Long approvalRequestId;
    private OffsetDateTime destroyedAt;

    @EnumValue
    private DestroyMethod destroyMethod;

    @TableField("supervisor_name_1")
    private String supervisorName1;
    @TableField("supervisor_name_2")
    private String supervisorName2;
    private String destroyNote;
}
