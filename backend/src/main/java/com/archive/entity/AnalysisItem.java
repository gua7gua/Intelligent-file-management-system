package com.archive.entity;

import com.archive.enums.AnalysisIssueType;
import com.archive.enums.AnalysisItemStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 研判异常项实体（analysis_items）。issue_detail / suggestion 为 JSONB。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "analysis_items", autoResultMap = true)
public class AnalysisItem extends BaseEntity {

    private Long taskId;

    private Long archiveId;

    @EnumValue
    private AnalysisIssueType issueType;

    /** 异常详情 JSON。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> issueDetail;

    /** AI 建议 JSON（只写不改档案）。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> suggestion;

    @EnumValue
    private AnalysisItemStatus status;

    private Long handledBy;

    private OffsetDateTime handledAt;

    private OffsetDateTime deletedAt;
}
