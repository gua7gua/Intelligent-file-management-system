package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 研判任务响应（§20.4/20.5）。
 *
 * <p>既有字段（taskNo/taskType/status/latestAiTaskId/startedAt/completedAt）保留，
 * 同时补齐前端研判页（原型 data-analysis.html）所需的 scopeText/scannedCount/
 * abnormalCount/adoptedCount/progress/createdAt。</p>
 */
@Data
public class AnalysisTaskResponse {

    private Long id;
    private String taskNo;
    private String taskType;
    /** 扫描方式（rule/ai/mixed）。 */
    private String scanMethod;
    private String status;
    private Long latestAiTaskId;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    /** 任务范围摘要（如：科技档案 / 文书档案 2010~2026）。 */
    private String scopeText;
    /** 已扫描档案数。 */
    private long scannedCount;
    /** 异常项总数。 */
    private long abnormalCount;
    /** 已采纳项数。 */
    private long adoptedCount;
    /** 进度 0~1。 */
    private double progress;
    /** 创建时间。 */
    private OffsetDateTime createdAt;
}
