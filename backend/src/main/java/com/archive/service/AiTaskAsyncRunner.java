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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

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
            String system = """
                    你是档案数据研判助手，只输出标签类建议，禁止输出密级/保管期限/开放状态/档号等受保护字段。
                    对每个档案，基于其题名、门类、责任者、形成年度以及【现有标签】：
                    1. 判断现有标签是否与档案门类明显冲突（例如会计档案却挂"影像资料""社会捐赠"等与门类无关的标签）；
                    2. 推测应补充的标签。

                    门类对照（code=名称）：accounting=会计档案、audio_video=音像档案、document=文书档案、technology=科技档案、personnel=人事档案。

                    输出 JSON（用 <JSON></JSON> 包裹）：
                    {"ruleType":"dataAnalysis","items":[
                      {"archiveId":1,"candidates":[
                        {"field":"tags","currentValue":"<现有标签逗号串或空字符串>","suggestedValue":["标签1","标签2"],"verdict":"ok|wrong","reason":"<verdict=wrong 时说明冲突原因，否则空字符串>","confidence":0.8}
                      ]}
                    ]}

                    要求：
                    - field 固定为 "tags"。
                    - verdict="wrong" 表示现有标签与门类明显冲突（错标签，需纠正）；"ok" 表示正常（仅建议补充）。
                    - suggestedValue 是字符串数组，2~5 个标签，优先依据题名/门类/责任者/年度，避免臆造。
                    - currentValue 为该档案现有标签的逗号串或空字符串。
                    - confidence 取值 0~1，反映对该建议的把握程度。
                    - 只输出本批提供的 archiveId，不得编造。
                    """;
            StringBuilder user = new StringBuilder("为下列档案判断标签是否错配并建议补充标签：\n");
            for (Archive a : archives) {
                user.append("- archiveId: ").append(a.getId());
                if (a.getTitle() != null) user.append(" 题名:").append(a.getTitle());
                String cat = lookupCategoryName(a.getCategoryId());
                if (cat != null) user.append(" 门类:").append(cat);
                if (a.getResponsibleText() != null) user.append(" 责任者:").append(a.getResponsibleText());
                String existing = lookupExistingTags(a.getId());
                user.append(" 现有标签:").append(existing.isEmpty() ? "无" : existing);
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

    /** 按 categoryId 查门类名称（供 prompt 让 AI 理解门类）。 */
    private String lookupCategoryName(Integer categoryId) {
        if (categoryId == null) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT category_name FROM categories WHERE id = ?", String.class, categoryId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 查档案现有标签逗号串（供 AI 判断标签是否错配）。 */
    private String lookupExistingTags(Long archiveId) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT t.tag_name FROM archive_tags at JOIN tags t ON t.id = at.tag_id WHERE at.archive_id = ?",
                    String.class, archiveId);
            return names != null ? String.join(",", names) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** AI 只写 suggestion（tag_suggestion/tag_wrong 项），不修改正式档案字段。analysisTaskId 取自 ai_tasks.business_id。
     *  suggestion 顶层按 candidates 结构写入，供前端 AI 建议区渲染；同时保留 suggestedTags 兼容旧读取方。
     *  candidate.verdict="wrong" 时写入 tag_wrong 类型（标签错配），否则 tag_suggestion。 */
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
            // 兼容 AI 同时返回 suggestedTags 或 candidates 两种形态
            List<String> tagList = new ArrayList<>();
            JsonNode tags = it.get("suggestedTags");
            if (tags != null && tags.isArray()) {
                tags.forEach(x -> { if (x.isTextual()) tagList.add(x.asText()); });
            }
            JsonNode candidates = it.get("candidates");
            // 预扫 candidates 判断是否有 wrong verdict（标签错配）：
            // 即使 AI 未给出纠正标签，错配项也应记录为 tag_wrong，不得跳过。
            boolean hasWrongVerdict = false;
            if (candidates != null && candidates.isArray()) {
                for (JsonNode c : candidates) {
                    JsonNode f = c.get("field");
                    if (f == null || !"tags".equals(f.asText())) continue;
                    JsonNode v = c.get("verdict");
                    if (v != null && v.isTextual() && "wrong".equals(v.asText())) {
                        hasWrongVerdict = true;
                    }
                }
            }
            if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
                // 没有 candidates 但有 suggestedTags，降级构造一个 tags 候选
                if (tagList.isEmpty() && !hasWrongVerdict) continue;
                candidates = null;
            } else {
                // 从 candidates 反向补全 tagList（保证 suggestedTags 兼容）
                for (JsonNode c : candidates) {
                    JsonNode f = c.get("field");
                    if (f != null && "tags".equals(f.asText())) {
                        JsonNode sv = c.get("suggestedValue");
                        if (sv != null && sv.isArray() && tagList.isEmpty()) {
                            sv.forEach(x -> { if (x.isTextual()) tagList.add(x.asText()); });
                        }
                    }
                }
            }
            if (tagList.isEmpty() && !hasWrongVerdict) continue;

            String archiveNo = null;
            try {
                Archive a = archiveMapper.selectById(aid);
                if (a != null) archiveNo = a.getArchiveNo();
            } catch (Exception ignored) {
                // 单条异常不影响整体持久化
            }

            // candidates 数组：若 AI 已提供则透传，否则按 tagList 构造；同时读取 verdict/reason 判断标签错配
            List<Map<String, Object>> candidateList = new ArrayList<>();
            String wrongReason = null;
            if (candidates != null) {
                for (JsonNode c : candidates) {
                    JsonNode f = c.get("field");
                    if (f == null || !"tags".equals(f.asText())) continue;
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("field", "tags");
                    JsonNode cv = c.get("currentValue");
                    m.put("currentValue", cv != null && cv.isTextual() ? cv.asText() : "");
                    // suggestedValue 统一输出字符串数组
                    List<String> svList = new ArrayList<>();
                    JsonNode sv = c.get("suggestedValue");
                    if (sv != null && sv.isArray()) {
                        sv.forEach(x -> { if (x.isTextual()) svList.add(x.asText()); });
                    } else if (sv != null && sv.isTextual()) {
                        // 容错：AI 偶发以字符串形式返回
                        svList.add(sv.asText());
                    }
                    if (svList.isEmpty()) svList.addAll(tagList);
                    m.put("suggestedValue", svList);
                    JsonNode conf = c.get("confidence");
                    m.put("confidence", conf != null && conf.isNumber() ? conf.asDouble() : 0.8);
                    JsonNode verdictNode = c.get("verdict");
                    String verdictStr = verdictNode != null && verdictNode.isTextual() ? verdictNode.asText() : "ok";
                    m.put("verdict", verdictStr);
                    JsonNode reasonNode = c.get("reason");
                    if (reasonNode != null && reasonNode.isTextual() && !reasonNode.asText().isBlank()) {
                        m.put("reason", reasonNode.asText());
                        if ("wrong".equals(verdictStr) && wrongReason == null) {
                            wrongReason = reasonNode.asText();
                        }
                    }
                    candidateList.add(m);
                }
            }
            if (candidateList.isEmpty()) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("field", "tags");
                m.put("currentValue", "");
                m.put("suggestedValue", tagList);
                m.put("confidence", 0.8);
                m.put("verdict", hasWrongVerdict ? "wrong" : "ok");
                candidateList.add(m);
            }

            Map<String, Object> suggestion = new java.util.LinkedHashMap<>();
            suggestion.put("ruleType", "dataAnalysis");
            suggestion.put("archiveId", aid);
            if (archiveNo != null) suggestion.put("archiveNo", archiveNo);
            suggestion.put("candidates", candidateList);
            suggestion.put("suggestedTags", tagList); // 兼容旧读取方
            if (hasWrongVerdict) {
                suggestion.put("verdict", "wrong");
                if (wrongReason != null) suggestion.put("wrongReason", wrongReason);
            }

            AnalysisItem item = new AnalysisItem();
            item.setTaskId(analysisTaskId);
            item.setArchiveId(aid);
            item.setIssueType(hasWrongVerdict ? AnalysisIssueType.tag_wrong : AnalysisIssueType.tag_suggestion);
            Map<String, Object> detail = new java.util.LinkedHashMap<>();
            detail.put("source", "ai");
            detail.put("aiTaskId", aiTaskId);
            if (hasWrongVerdict) {
                detail.put("wrongReason", wrongReason != null ? wrongReason : "现有标签与档案门类冲突");
            }
            item.setIssueDetail(detail);
            item.setSuggestion(suggestion);
            item.setStatus(AnalysisItemStatus.pending);
            analysisItemMapper.insert(item);
        }
    }
}
