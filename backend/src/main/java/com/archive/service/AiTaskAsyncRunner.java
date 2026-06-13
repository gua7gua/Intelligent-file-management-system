package com.archive.service;

import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeItem;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.IntakeItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI 补全异步执行器。
 * 独立 bean 持有 @Async 方法，避免同类 self-invocation 导致异步失效。
 * 逐批调用 AiClient + AiSuggestionService，独立记录成败，最后汇总任务状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskAsyncRunner {

    private final AiTaskMapper aiTaskMapper;
    private final AiTaskBatchMapper aiTaskBatchMapper;
    private final IntakeItemMapper intakeItemMapper;
    private final AiClient aiClient;
    private final AiSuggestionService aiSuggestionService;

    /** 异步执行任务的所有 pending/failed 批次。 */
    @Async("aiTaskExecutor")
    public void executeIntakeCompletion(Long taskId) {
        try {
            List<AiTaskBatch> batches = aiTaskBatchMapper.selectList(new QueryWrapper<AiTaskBatch>()
                    .eq("task_id", taskId)
                    .in("status", "pending", "failed")
                    .orderByAsc("batch_no"));
            for (AiTaskBatch batch : batches) {
                processBatch(batch);
            }
            summarizeTask(taskId);
        } catch (Exception e) {
            log.error("AI 任务 {} 异步执行异常", taskId, e);
            AiTask t = aiTaskMapper.selectById(taskId);
            if (t != null && "running".equals(t.getStatus())) {
                t.setStatus("failed");
                t.setErrorMessage("任务执行异常: " + e.getMessage());
                t.setCompletedAt(OffsetDateTime.now());
                aiTaskMapper.updateById(t);
            }
        }
    }

    private void processBatch(AiTaskBatch batch) {
        batch.setStatus("running");
        batch.setStartedAt(OffsetDateTime.now());
        batch.setAttemptCount((batch.getAttemptCount() == null ? 0 : batch.getAttemptCount()) + 1);
        aiTaskBatchMapper.updateById(batch);
        try {
            List<IntakeItem> items = intakeItemMapper.selectBatchIds(batch.getTargetIds());
            String systemPrompt = aiSuggestionService.buildSystemPrompt();
            String userMessage = aiSuggestionService.buildUserMessage(items);
            JsonNode aiResult = aiClient.callAndExtractJson(systemPrompt, userMessage);
            batch.setRawResponse(aiResult.toString());
            aiSuggestionService.validateAndPersist(aiResult, batch); // 内部设置 validatedResult
            batch.setStatus("success");
        } catch (Exception e) {
            log.warn("AI 批次 {} 执行失败: {}", batch.getId(), e.getMessage());
            batch.setStatus("failed");
            batch.setErrorMessage(e.getMessage());
        }
        batch.setCompletedAt(OffsetDateTime.now());
        aiTaskBatchMapper.updateById(batch);
    }

    private void summarizeTask(Long taskId) {
        List<AiTaskBatch> all = aiTaskBatchMapper.selectList(
                new QueryWrapper<AiTaskBatch>().eq("task_id", taskId));
        int success = 0, failed = 0;
        for (AiTaskBatch b : all) {
            if ("success".equals(b.getStatus())) success++;
            else if ("failed".equals(b.getStatus())) failed++;
        }
        AiTask t = aiTaskMapper.selectById(taskId);
        if (t == null) return;
        t.setSuccessBatches(success);
        t.setFailedBatches(failed);
        t.setStatus(summarizeStatus(success, failed));
        t.setCompletedAt(OffsetDateTime.now());
        aiTaskMapper.updateById(t);
    }

    /** 纯逻辑：根据成功/失败批次数计算任务状态。 */
    static String summarizeStatus(int success, int failed) {
        if (success > 0 && failed > 0) return "partial_completed";
        if (success > 0) return "completed";
        return "failed";
    }
}
