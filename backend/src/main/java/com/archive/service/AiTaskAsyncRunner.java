package com.archive.service;

import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.AnalysisItem;
import com.archive.entity.AnalysisTask;
import com.archive.entity.Archive;
import com.archive.entity.IntakeItem;
import com.archive.enums.AnalysisIssueType;
import com.archive.enums.AnalysisItemStatus;
import com.archive.enums.AnalysisTaskStatus;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.AnalysisItemMapper;
import com.archive.mapper.AnalysisTaskMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.IntakeItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final ArchiveMapper archiveMapper;
    private final AnalysisItemMapper analysisItemMapper;
    private final AnalysisTaskMapper analysisTaskMapper;

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

    // ==================== 研判 AI（archive_analysis） ====================

    /** 异步执行研判 AI 任务的所有 pending/failed 批次，并回写研判任务状态。 */
    @Async("aiTaskExecutor")
    public void executeArchiveAnalysis(Long aiTaskId, Long analysisTaskId) {
        try {
            List<AiTaskBatch> batches = aiTaskBatchMapper.selectList(new QueryWrapper<AiTaskBatch>()
                    .eq("task_id", aiTaskId)
                    .in("status", "pending", "failed")
                    .orderByAsc("batch_no"));
            for (AiTaskBatch b : batches) {
                processAnalysisBatch(b);
            }
            summarizeTask(aiTaskId);

            AiTask at = aiTaskMapper.selectById(aiTaskId);
            String st = at != null && at.getStatus() != null ? at.getStatus() : "failed";
            AnalysisTask t = analysisTaskMapper.selectById(analysisTaskId);
            if (t != null && t.getStatus() == AnalysisTaskStatus.running) {
                AnalysisTaskStatus mapped = "failed".equals(st) ? AnalysisTaskStatus.completed
                        : AnalysisTaskStatus.partial_completed.name().equals(st) ? AnalysisTaskStatus.partial_completed
                        : AnalysisTaskStatus.completed;
                t.setStatus(mapped);
                t.setCompletedAt(OffsetDateTime.now());
                analysisTaskMapper.updateById(t);
            }
        } catch (Exception e) {
            log.error("研判 AI 任务 {} 异步执行异常", aiTaskId, e);
            AiTask at = aiTaskMapper.selectById(aiTaskId);
            if (at != null && "running".equals(at.getStatus())) {
                at.setStatus("failed");
                at.setErrorMessage("研判任务执行异常: " + e.getMessage());
                at.setCompletedAt(OffsetDateTime.now());
                aiTaskMapper.updateById(at);
            }
        }
    }

    private void processAnalysisBatch(AiTaskBatch batch) {
        batch.setStatus("running");
        batch.setStartedAt(OffsetDateTime.now());
        batch.setAttemptCount((batch.getAttemptCount() == null ? 0 : batch.getAttemptCount()) + 1);
        aiTaskBatchMapper.updateById(batch);
        try {
            List<Archive> archives = archiveMapper.selectBatchIds(batch.getTargetIds());
            String system = "你是档案数据研判助手。只能输出标签类建议，"
                    + "禁止输出密级/保管期限/开放状态/档号等受保护字段。"
                    + "输出 <JSON>{\"items\":[{\"archiveId\":1,\"suggestedTags\":[\"标签\"]}]}</JSON>。";
            StringBuilder user = new StringBuilder("为下列档案建议补充标签：\n");
            for (Archive a : archives) {
                user.append("- archiveId: ").append(a.getId());
                if (a.getTitle() != null) user.append(" 题名:").append(a.getTitle());
                if (a.getCategoryId() != null) user.append(" 门类:").append(a.getCategoryId());
                user.append('\n');
            }
            JsonNode res = aiClient.callAndExtractJson(system, user.toString());
            batch.setRawResponse(res != null ? res.toString() : null);
            persistAnalysisSuggestions(batch.getTaskId(), res);
            batch.setStatus("success");
        } catch (Exception e) {
            log.warn("研判批次 {} 执行失败: {}", batch.getId(), e.getMessage());
            batch.setStatus("failed");
            batch.setErrorMessage(e.getMessage());
        }
        batch.setCompletedAt(OffsetDateTime.now());
        aiTaskBatchMapper.updateById(batch);
    }

    /** AI 只写 suggestion（tag_suggestion 项），不修改正式档案字段。analysisTaskId 取自 ai_tasks.business_id。 */
    private void persistAnalysisSuggestions(Long aiTaskId, JsonNode res) {
        if (res == null) return;
        JsonNode items = res.get("items");
        if (items == null || !items.isArray()) return;
        AiTask at = aiTaskMapper.selectById(aiTaskId);
        Long analysisTaskId = at != null ? at.getBusinessId() : null;
        for (JsonNode it : items) {
            JsonNode idNode = it.get("archiveId");
            if (idNode == null || !idNode.canConvertToLong()) continue;
            long aid = idNode.asLong();
            JsonNode tags = it.get("suggestedTags");
            if (tags == null || !tags.isArray() || tags.isEmpty()) continue;
            List<String> tagList = new ArrayList<>();
            tags.forEach(x -> { if (x.isTextual()) tagList.add(x.asText()); });
            if (tagList.isEmpty()) continue;
            AnalysisItem item = new AnalysisItem();
            item.setTaskId(analysisTaskId);
            item.setArchiveId(aid);
            item.setIssueType(AnalysisIssueType.tag_suggestion);
            item.setIssueDetail(Map.of("source", "ai", "aiTaskId", aiTaskId));
            item.setSuggestion(Map.of("suggestedTags", tagList));
            item.setStatus(AnalysisItemStatus.pending);
            analysisItemMapper.insert(item);
        }
    }
}
