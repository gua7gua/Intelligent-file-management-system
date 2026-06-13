package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 任务批次。每个批次独立调用 AI、独立校验、独立记录成败。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_task_batches", autoResultMap = true)
public class AiTaskBatch extends BaseEntity {

    /** 关联 ai_tasks.id */
    private Long taskId;
    /** 任务内批次序号，从 1 递增 */
    private Integer batchNo;
    /** 批次状态：pending / running / success / failed */
    private String status;
    /** 本批处理的清单条目 ID 或档案 ID */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetIds;
    /** 本批注入的上下文摘要，不含敏感密钥 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> requestContext;
    /** AI 原始响应，便于排错 */
    private String rawResponse;
    /** 通过校验后的结果 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validatedResult;
    /** 批次错误说明 */
    private String errorMessage;
    /** 尝试次数 */
    private Integer attemptCount;
    /** 批次开始时间 */
    private OffsetDateTime startedAt;
    /** 批次完成时间 */
    private OffsetDateTime completedAt;
}
