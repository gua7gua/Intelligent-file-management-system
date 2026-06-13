package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * AI 任务主表。记录检索类与批处理类 AI 调用的任务汇总。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_tasks")
public class AiTask extends BaseEntity {

    /** 任务号，AIT-{6位序号} */
    private String taskNo;
    /** 任务类型：intake_completion / internal_search_query / public_search_query / archive_analysis */
    private String taskType;
    /** 业务对象类型：intake_batch / analysis_task / none */
    private String businessType;
    /** 业务对象 ID；检索类可空 */
    private Long businessId;
    /** 任务状态：running / partial_completed / completed / failed */
    private String status;
    /** 批大小 */
    private Integer batchSize;
    /** 总批次数 */
    private Integer totalBatches;
    /** 成功批次数 */
    private Integer successBatches;
    /** 失败批次数 */
    private Integer failedBatches;
    /** 开始时间 */
    private OffsetDateTime startedAt;
    /** 完成时间 */
    private OffsetDateTime completedAt;
    /** 任务级错误摘要 */
    private String errorMessage;
}
