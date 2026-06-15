package com.archive.entity;

import com.archive.enums.AnalysisTaskStatus;
import com.archive.enums.AnalysisTaskType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 研判任务实体（analysis_tasks）。rule_snapshot 为 JSONB。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "analysis_tasks", autoResultMap = true)
public class AnalysisTask extends BaseEntity {

    private String taskNo;

    @EnumValue
    private AnalysisTaskType taskType;

    @EnumValue
    private AnalysisTaskStatus status;

    /** 规则快照 JSON：categoryIds/formedYearStart/formedYearEnd/includeAiSuggestion。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> ruleSnapshot;

    private Long latestAiTaskId;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    private OffsetDateTime deletedAt;
}
