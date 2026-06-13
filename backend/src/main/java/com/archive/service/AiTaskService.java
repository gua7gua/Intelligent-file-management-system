package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.AiTaskResponse;
import com.archive.dto.response.AiTaskStartResponse;
import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.exception.BusinessException;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.archive.util.AiTaskNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 任务管理服务。
 * 负责任务/批次创建、拆批、查询、失败重试编排；真正的异步逐批执行在 AiTaskAsyncRunner。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskService {

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskBatchMapper aiTaskBatchMapper;
    private final IntakeItemMapper intakeItemMapper;
    private final IntakeBatchMapper intakeBatchMapper;
    private final AiClient aiClient;
    private final AiTaskNoUtil aiTaskNoUtil;
    private final AuditService auditService;
    private final AiTaskAsyncRunner asyncRunner;

    /** 把 id 列表按 batchSize 拆成多批，纯逻辑。 */
    static List<List<Long>> splitIntoBatches(List<Long> ids, int batchSize) {
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += batchSize) {
            out.add(new ArrayList<>(ids.subList(i, Math.min(i + batchSize, ids.size()))));
        }
        return out;
    }

    /** 9.3 启动 AI 补全。 */
    public AiTaskStartResponse startIntakeCompletion(Long batchId) {
        if (!aiClient.isAvailable()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI 功能未启用");
        }
        IntakeBatch batch = intakeBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单批次不存在");
        }
        List<IntakeItem> items = intakeItemMapper.selectList(new QueryWrapper<IntakeItem>()
                .eq("batch_id", batchId)
                .in("status", "accepted", "pending_archive")
                .isNull("generated_archive_id"));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "批次没有可补全的条目");
        }
        Long running = aiTaskMapper.selectCount(new QueryWrapper<AiTask>()
                .eq("business_type", "intake_batch").eq("business_id", batchId).eq("status", "running"));
        if (running != null && running > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该批次已有运行中的 AI 任务");
        }

        int batchSize = aiClient.getBatchSize();
        List<List<Long>> batches = splitIntoBatches(
                items.stream().map(IntakeItem::getId).toList(), batchSize);

        AiTask task = new AiTask();
        task.setTaskNo(aiTaskNoUtil.generate());
        task.setTaskType("intake_completion");
        task.setBusinessType("intake_batch");
        task.setBusinessId(batchId);
        task.setStatus("running");
        task.setBatchSize(batchSize);
        task.setTotalBatches(batches.size());
        task.setSuccessBatches(0);
        task.setFailedBatches(0);
        task.setStartedAt(OffsetDateTime.now());
        aiTaskMapper.insert(task);

        for (int i = 0; i < batches.size(); i++) {
            AiTaskBatch b = new AiTaskBatch();
            b.setTaskId(task.getId());
            b.setBatchNo(i + 1);
            b.setStatus("pending");
            b.setTargetIds(batches.get(i));
            b.setAttemptCount(0);
            aiTaskBatchMapper.insert(b);
        }

        auditService.log("M04", "start_ai_completion", "intake_batch", batchId,
                Map.of("aiTaskId", task.getId(), "totalBatches", batches.size()));

        asyncRunner.executeIntakeCompletion(task.getId());

        return new AiTaskStartResponse(task.getId(), task.getTaskNo(), task.getStatus(),
                batchSize, batches.size());
    }

    /** 9.5 重试失败批次。 */
    public AiTaskResponse retryFailed(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 任务不存在");
        }
        if (!"intake_completion".equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非清单补全任务");
        }
        if (!"partial_completed".equals(task.getStatus()) && !"failed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前任务状态不允许重试");
        }
        List<AiTaskBatch> failedBatches = aiTaskBatchMapper.selectList(new QueryWrapper<AiTaskBatch>()
                .eq("task_id", taskId).eq("status", "failed"));
        if (failedBatches.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "没有可重试的失败批次");
        }
        for (AiTaskBatch b : failedBatches) {
            b.setStatus("pending");
            aiTaskBatchMapper.updateById(b);
        }
        task.setStatus("running");
        task.setErrorMessage(null);
        aiTaskMapper.updateById(task);
        auditService.log("M04", "retry_ai_failed", "ai_task", taskId,
                Map.of("failedBatches", failedBatches.size()));
        asyncRunner.executeIntakeCompletion(taskId);
        return getTask(taskId);
    }

    /** 9.4 查询任务。 */
    public AiTaskResponse getTask(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 任务不存在");
        }
        return new AiTaskResponse(task.getId(), task.getTaskNo(), task.getStatus(),
                task.getTotalBatches(), task.getSuccessBatches(), task.getFailedBatches(),
                task.getErrorMessage(), task.getStartedAt(), task.getCompletedAt());
    }
}
