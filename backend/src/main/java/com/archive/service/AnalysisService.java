package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.AnalysisItemHandleRequest;
import com.archive.dto.request.AnalysisRule;
import com.archive.dto.request.AnalysisTaskCreateRequest;
import com.archive.dto.request.AnalysisTaskQuery;
import com.archive.dto.response.AnalysisItemResponse;
import com.archive.dto.response.AnalysisTaskDetailResponse;
import com.archive.dto.response.AnalysisTaskResponse;
import com.archive.entity.AnalysisItem;
import com.archive.entity.AnalysisTask;
import com.archive.entity.Archive;
import com.archive.enums.AnalysisIssueType;
import com.archive.enums.AnalysisItemStatus;
import com.archive.enums.AnalysisTaskStatus;
import com.archive.enums.AnalysisTaskType;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.AnalysisItemMapper;
import com.archive.mapper.AnalysisTaskMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.util.AnalysisTaskNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据研判服务（M14）。
 * 规则扫描始终产出异常候选；AI 建议为增强项，只写 analysis_items.suggestion，不直接改档案。
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisTaskMapper taskMapper;
    private final AnalysisItemMapper itemMapper;
    private final ArchiveMapper archiveMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AnalysisTaskNoUtil noUtil;
    private final AiTaskService aiTaskService;
    private final AuditService auditService;

    private void requireRole() {
        if (!AuthContext.hasRole(RoleCode.back_archivist)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /** 20.4 查询研判任务 */
    public PageResult<AnalysisTaskResponse> list(AnalysisTaskQuery q) {
        requireRole();
        QueryWrapper<AnalysisTask> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (q.getStatus() != null) w.eq("status", q.getStatus());
        if (q.getTaskType() != null) w.eq("task_type", q.getTaskType());
        w.orderByDesc("created_at");
        Page<AnalysisTask> p = taskMapper.selectPage(new Page<>(q.getPageNo(), q.getPageSize()), w);
        return new PageResult<>(p.getRecords().stream().map(this::toResponse).toList(),
                q.getPageNo(), q.getPageSize(), p.getTotal());
    }

    /** 20.5 创建研判任务：规则扫描 + 可选 AI */
    @Transactional
    public AnalysisTaskResponse create(AnalysisTaskCreateRequest req) {
        requireRole();
        AnalysisRule rule = req.getRule() != null ? req.getRule() : new AnalysisRule();
        List<Long> archiveIds = scopedArchiveIds(rule);
        if (archiveIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "规则范围内没有可研判档案");
        }

        AnalysisTask t = new AnalysisTask();
        t.setTaskNo(noUtil.generate());
        // 方案 A：scan_method 记录用户选择的扫描方式，task_type 固定为 mixed
        // （规则扫描统一覆盖缺字段 + 门类冲突两种检查，结果类型为综合）。
        t.setScanMethod(req.getScanMethod());
        t.setTaskType(AnalysisTaskType.mixed);
        t.setStatus(AnalysisTaskStatus.running);
        // 用 LinkedHashMap 构造规则快照（允许 null 值，Map.of 会拒绝 null）
        Map<String, Object> snap = new java.util.LinkedHashMap<>();
        snap.put("categoryIds", rule.getCategoryIds() != null ? rule.getCategoryIds() : List.of());
        snap.put("formedYearStart", rule.getFormedYearStart());
        snap.put("formedYearEnd", rule.getFormedYearEnd());
        snap.put("includeAiSuggestion", Boolean.TRUE.equals(rule.getIncludeAiSuggestion()));
        t.setRuleSnapshot(snap);
        t.setStartedAt(OffsetDateTime.now());
        taskMapper.insert(t);

        runRuleScan(t.getId(), AnalysisTaskType.mixed, archiveIds);

        boolean wantAi = Boolean.TRUE.equals(rule.getIncludeAiSuggestion());
        Long aiTaskId = null;
        if (wantAi && aiTaskService.aiAvailable()) {
            aiTaskId = aiTaskService.startArchiveAnalysis(t.getId(), archiveIds);
            t.setLatestAiTaskId(aiTaskId);
            t.setStatus(AnalysisTaskStatus.running);
        } else {
            t.setStatus(AnalysisTaskStatus.completed);
            t.setCompletedAt(OffsetDateTime.now());
        }
        taskMapper.updateById(t);

        // 事务提交后再触发异步 AI，避免异步线程读不到未提交的 ai_task_batches
        // （@Async + @Transactional 的可见性竞态）。
        if (aiTaskId != null) {
            final Long atid = t.getId();
            final Long aiTid = aiTaskId;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        aiTaskService.startArchiveAnalysisAsync(aiTid, atid);
                    }
                });
            } else {
                aiTaskService.startArchiveAnalysisAsync(aiTid, atid);
            }
        }
        auditService.log("M14", "create_analysis_task", "analysis_task", t.getId(),
                Map.of("scanMethod", req.getScanMethod().name(),
                        "taskType", AnalysisTaskType.mixed.name(),
                        "archives", archiveIds.size()));
        return toResponse(t);
    }

    /** 规则扫描：缺字段 + 门类冲突（公开却带密级视为冲突）。 */
    private void runRuleScan(Long taskId, AnalysisTaskType type, List<Long> archiveIds) {
        for (Long aid : archiveIds) {
            Archive a = archiveMapper.selectById(aid);
            if (a == null) continue;
            if (type == AnalysisTaskType.missing_fields || type == AnalysisTaskType.mixed) {
                List<String> missing = new ArrayList<>();
                if (isBlank(a.getTitle())) missing.add("title");
                if (isBlank(a.getResponsibleText())) missing.add("responsible");
                if (a.getFormedDate() == null) missing.add("formedDate");
                if (!missing.isEmpty()) {
                    insertItem(taskId, aid, AnalysisIssueType.missing_field,
                            Map.of("fields", missing), null);
                }
            }
            if (type == AnalysisTaskType.category_conflict || type == AnalysisTaskType.mixed) {
                boolean openButClassified = "open".equals(a.getOpenStatus())
                        && a.getSecurityLevel() != null && a.getSecurityLevel() > 0;
                if (openButClassified) {
                    insertItem(taskId, aid, AnalysisIssueType.category_conflict,
                            Map.of("reason", "公开状态但存在密级"), null);
                }
            }
        }
    }

    private void insertItem(Long taskId, Long archiveId, AnalysisIssueType it,
                            Map<String, Object> detail, Map<String, Object> suggestion) {
        AnalysisItem item = new AnalysisItem();
        item.setTaskId(taskId);
        item.setArchiveId(archiveId);
        item.setIssueType(it);
        item.setIssueDetail(detail);
        item.setSuggestion(suggestion);
        item.setStatus(AnalysisItemStatus.pending);
        itemMapper.insert(item);
    }

    private List<Long> scopedArchiveIds(AnalysisRule rule) {
        StringBuilder sql = new StringBuilder(
                "SELECT id FROM archives WHERE deleted_at IS NULL AND lifecycle_status<>'destroyed'" +
                        " AND analysis_ignored_at IS NULL");
        List<Object> args = new ArrayList<>();
        if (rule.getCategoryIds() != null && !rule.getCategoryIds().isEmpty()) {
            sql.append(" AND category_id IN (");
            for (int i = 0; i < rule.getCategoryIds().size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
            }
            args.addAll(rule.getCategoryIds());
            sql.append(")");
        }
        if (rule.getFormedYearStart() != null) {
            sql.append(" AND formed_year>=?");
            args.add(rule.getFormedYearStart());
        }
        if (rule.getFormedYearEnd() != null) {
            sql.append(" AND formed_year<=?");
            args.add(rule.getFormedYearEnd());
        }
        return jdbcTemplate.queryForList(sql.toString(), Long.class, args.toArray());
    }

    /** 20.6 详情 */
    public AnalysisTaskDetailResponse getDetail(Long taskId) {
        requireRole();
        AnalysisTask t = taskMapper.selectById(taskId);
        if (t == null || t.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "研判任务不存在");
        }
        List<AnalysisItem> items = itemMapper.selectList(new QueryWrapper<AnalysisItem>()
                .eq("task_id", taskId).isNull("deleted_at").orderByAsc("id"));
        AnalysisTaskDetailResponse resp = new AnalysisTaskDetailResponse();
        copy(t, resp);
        enrichTaskStats(t, resp, items);
        resp.setRule(toRule(t.getRuleSnapshot()));
        resp.setItems(items.stream().map(this::toItemResponse).toList());
        return resp;
    }

    /** 从 rule_snapshot 还原 AnalysisRule（前端 detail.rule 需要）。 */
    private AnalysisRule toRule(Map<String, Object> snap) {
        if (snap == null) {
            return null;
        }
        AnalysisRule r = new AnalysisRule();
        Object cids = snap.get("categoryIds");
        if (cids instanceof List<?> list) {
            List<Integer> ids = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Number n) ids.add(n.intValue());
            }
            r.setCategoryIds(ids);
        }
        Object ys = snap.get("formedYearStart");
        if (ys instanceof Number n) r.setFormedYearStart(n.intValue());
        Object ye = snap.get("formedYearEnd");
        if (ye instanceof Number n) r.setFormedYearEnd(n.intValue());
        Object ai = snap.get("includeAiSuggestion");
        if (ai instanceof Boolean b) r.setIncludeAiSuggestion(b);
        return r;
    }

    /** 20.7 处理研判项（仅改状态，不动档案） */
    @Transactional
    public AnalysisItemResponse handleItem(Long itemId, AnalysisItemHandleRequest req) {
        requireRole();
        if (!"adopted".equals(req.getAction()) && !"rejected".equals(req.getAction())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "action 必须为 adopted 或 rejected");
        }
        AnalysisItem it = itemMapper.selectById(itemId);
        if (it == null || it.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "研判项不存在");
        }
        if (it.getStatus() != AnalysisItemStatus.pending) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "研判项已处理");
        }
        boolean rejected = "rejected".equals(req.getAction());
        it.setStatus(rejected ? AnalysisItemStatus.rejected : AnalysisItemStatus.adopted);
        it.setHandledBy(AuthContext.getCurrentUserId());
        it.setHandledAt(OffsetDateTime.now());
        itemMapper.updateById(it);
        // 不采纳=该档案无问题：打永久跳过标记，下次扫描不再纳入候选；
        // （删除则不走这里，仅软删当前异常项，下次扫描仍可再次发现。）
        if (rejected && it.getArchiveId() != null) {
            jdbcTemplate.update("UPDATE archives SET analysis_ignored_at = ? WHERE id = ?",
                    OffsetDateTime.now(), it.getArchiveId());
        }
        auditService.log("M14", "handle_analysis_item", "analysis_item", itemId,
                Map.of("action", req.getAction()));
        return toItemResponse(it);
    }

    /** 20.9 删除研判异常项（软删置 deleted_at，保留数据；与任务级联软删一致策略）。 */
    @Transactional
    public void deleteItem(Long itemId) {
        requireRole();
        AnalysisItem it = itemMapper.selectById(itemId);
        if (it == null || it.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "研判项不存在");
        }
        it.setDeletedAt(OffsetDateTime.now());
        itemMapper.updateById(it);
        auditService.log("M14", "delete_analysis_item", "analysis_item", itemId, Map.of());
    }

    /** 20.8 删除研判任务（仅 completed/failed 可删；级联软删 analysis_items）。 */
    @Transactional
    public void delete(Long taskId) {
        requireRole();
        AnalysisTask t = taskMapper.selectById(taskId);
        if (t == null || t.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "研判任务不存在");
        }
        if (t.getStatus() == AnalysisTaskStatus.running) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "运行中的研判任务不可删除");
        }
        // 允许删除的终态：completed / partial_completed / failed（running 之外的都视为可清理）
        if (t.getStatus() != AnalysisTaskStatus.completed
                && t.getStatus() != AnalysisTaskStatus.partial_completed
                && t.getStatus() != AnalysisTaskStatus.failed) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许删除");
        }
        // 级联软删 analysis_items（保留数据，置 deleted_at）
        List<AnalysisItem> items = itemMapper.selectList(new QueryWrapper<AnalysisItem>()
                .eq("task_id", taskId).isNull("deleted_at"));
        OffsetDateTime now = OffsetDateTime.now();
        for (AnalysisItem it : items) {
            it.setDeletedAt(now);
            itemMapper.updateById(it);
        }
        t.setDeletedAt(now);
        taskMapper.updateById(t);
        auditService.log("M14", "delete", "analysis_task", taskId, Map.of());
    }

    private void copy(AnalysisTask t, AnalysisTaskResponse r) {
        r.setId(t.getId());
        r.setTaskNo(t.getTaskNo());
        r.setTaskType(t.getTaskType() != null ? t.getTaskType().name() : null);
        r.setScanMethod(t.getScanMethod() != null ? t.getScanMethod().name() : null);
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setLatestAiTaskId(t.getLatestAiTaskId());
        r.setStartedAt(t.getStartedAt());
        r.setCompletedAt(t.getCompletedAt());
        r.setCreatedAt(t.getCreatedAt());
        r.setProgress(switch (t.getStatus() == null ? AnalysisTaskStatus.running : t.getStatus()) {
            case completed, failed -> 1.0;
            case partial_completed -> 0.5;
            default -> t.getLatestAiTaskId() != null ? 0.5 : 1.0;
        });
        r.setScopeText(buildScopeText(t));
        fillAiStatus(t, r);
    }

    /** 回填 AI 建议执行状态：从 latestAiTaskId 关联的 ai_tasks/ai_task_batches 统计批次成败。 */
    private void fillAiStatus(AnalysisTask t, AnalysisTaskResponse r) {
        Long aiId = t.getLatestAiTaskId();
        if (aiId == null) {
            r.setAiStatus("skipped");
            r.setAiTotalBatches(0);
            r.setAiFailedBatches(0);
            return;
        }
        String aiStatus;
        try {
            aiStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM ai_tasks WHERE id = ?", String.class, aiId);
        } catch (Exception e) {
            aiStatus = null;
        }
        Long totalRaw = null, failedRaw = null;
        try {
            totalRaw = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ai_task_batches WHERE task_id = ?", Long.class, aiId);
            failedRaw = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ai_task_batches WHERE task_id = ? AND status = 'failed'", Long.class, aiId);
        } catch (Exception ignored) {
            // 查询异常时按未知处理，不影响主流程
        }
        long total = totalRaw != null ? totalRaw : 0L;
        long failed = failedRaw != null ? failedRaw : 0L;
        r.setAiTotalBatches(total);
        r.setAiFailedBatches(failed);
        String s;
        if ("failed".equals(aiStatus)) {
            s = "failed";
        } else if (total == 0) {
            s = "skipped";
        } else if (failed == 0) {
            s = "success";
        } else if (failed >= total) {
            s = "failed";
        } else {
            s = "partial";
        }
        r.setAiStatus(s);
    }

    /** 范围摘要：解析 rule_snapshot，例如「科技档案 / 文书档案 2010~2026」。 */
    private String buildScopeText(AnalysisTask t) {
        Map<String, Object> snap = t.getRuleSnapshot();
        if (snap == null) {
            return "正式档案";
        }
        StringBuilder sb = new StringBuilder();
        Object cids = snap.get("categoryIds");
        if (cids instanceof List<?> list && !list.isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Number n) ids.add(n.intValue());
            }
            if (!ids.isEmpty()) {
                List<String> names = jdbcTemplate.queryForList(
                        "SELECT category_name FROM categories WHERE id IN (" +
                        ids.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(",")) + ")",
                        String.class, ids.toArray());
                if (!names.isEmpty()) {
                    sb.append(String.join(" / ", names));
                }
            }
        }
        Object ys = snap.get("formedYearStart");
        Object ye = snap.get("formedYearEnd");
        if (ys instanceof Number && ye instanceof Number) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(((Number) ys).intValue()).append('~').append(((Number) ye).intValue());
        }
        return sb.length() == 0 ? "正式档案" : sb.toString();
    }

    /** 为任务响应填充扫描数/异常数/已采纳数（列表项复用统计）。 */
    private void enrichTaskStats(AnalysisTask t, AnalysisTaskResponse r, List<AnalysisItem> items) {
        // 扫描数：从 rule_snapshot 范围聚合（实时）
        long scanned = countScanned(t);
        long abnormal = items != null ? items.size() : countItems(t.getId(), null);
        long adopted = items != null ? items.stream().filter(i -> i.getStatus() == AnalysisItemStatus.adopted).count()
                : countItems(t.getId(), "adopted");
        r.setScannedCount(scanned);
        r.setAbnormalCount(abnormal);
        r.setAdoptedCount(adopted);
    }

    private long countScanned(AnalysisTask t) {
        try {
            AnalysisRule rule = toRule(t.getRuleSnapshot());
            if (rule == null) {
                rule = new AnalysisRule();
            }
            return scopedArchiveIds(rule).size();
        } catch (Exception e) {
            return 0;
        }
    }

    private long countItems(Long taskId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM analysis_items WHERE task_id=? AND deleted_at IS NULL");
        List<Object> args = new ArrayList<>();
        args.add(taskId);
        if (status != null) {
            sql.append(" AND status=?");
            args.add(status);
        }
        Long v = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return v != null ? v : 0L;
    }

    private AnalysisTaskResponse toResponse(AnalysisTask t) {
        AnalysisTaskResponse r = new AnalysisTaskResponse();
        copy(t, r);
        enrichTaskStats(t, r, null);
        return r;
    }

    private AnalysisItemResponse toItemResponse(AnalysisItem it) {
        AnalysisItemResponse r = new AnalysisItemResponse();
        r.setId(it.getId());
        r.setTaskId(it.getTaskId());
        r.setArchiveId(it.getArchiveId());
        String issue = it.getIssueType() != null ? it.getIssueType().name() : null;
        r.setIssueType(issue);
        r.setProblemType(issue);
        r.setIssueDetail(it.getIssueDetail());
        r.setSuggestion(it.getSuggestion());
        r.setStatus(it.getStatus() != null ? it.getStatus().name() : null);
        r.setHandledAt(it.getHandledAt());
        r.setHandledBy(it.getHandledBy());

        // 回填档案档号 / 题名 + 派生 problemDesc / suggestedAction
        if (it.getArchiveId() != null) {
            try {
                Archive a = archiveMapper.selectById(it.getArchiveId());
                if (a != null) {
                    r.setArchiveNo(a.getArchiveNo());
                    r.setTitle(a.getTitle());
                }
            } catch (Exception ignored) {
                // 单条异常不影响整体响应
            }
        }
        r.setProblemDesc(buildProblemDesc(it));
        r.setSuggestedAction(buildSuggestedAction(it));
        return r;
    }

    /** 按 issueType + issueDetail 派生人类可读的问题描述。 */
    private String buildProblemDesc(AnalysisItem it) {
        if (it.getIssueType() == null) {
            return "—";
        }
        Map<String, Object> d = it.getIssueDetail();
        switch (it.getIssueType()) {
            case missing_field -> {
                Object fields = d == null ? null : d.get("fields");
                return "缺失字段：" + (fields instanceof List<?> l && !l.isEmpty()
                        ? String.join("、", l.stream().map(String::valueOf).toList())
                        : "未知字段");
            }
            case category_conflict -> {
                Object reason = d == null ? null : d.get("reason");
                return reason == null ? "门类冲突" : String.valueOf(reason);
            }
            case tag_suggestion -> {
                Object reason = d == null ? null : d.get("reason");
                return reason == null ? "可补充标签建议" : String.valueOf(reason);
            }
            case tag_wrong -> {
                Object reason = d == null ? null : d.get("wrongReason");
                return reason == null ? "档案标签与门类明显冲突（标签错配）" : ("标签错配：" + String.valueOf(reason));
            }
            default -> {
                return "—";
            }
        }
    }

    /** 按 issueType 派生建议动作；tag_suggestion 附带具体建议标签值，便于直接采纳。 */
    private String buildSuggestedAction(AnalysisItem it) {
        if (it.getIssueType() == null) {
            return "—";
        }
        return switch (it.getIssueType()) {
            case missing_field -> "在档案详情页补全缺失字段后人工确认";
            case category_conflict -> "复核公开状态与密级，调整其中一项";
            case tag_suggestion -> buildTagSuggestionAction(it);
            case tag_wrong -> "核对档案门类，移除或修正与门类冲突的标签";
        };
    }

    /** 标签建议：从 suggestion.candidates[].suggestedValue（兼容旧 suggestedTags）抽取具体标签值。 */
    private String buildTagSuggestionAction(AnalysisItem it) {
        Map<String, Object> s = it.getSuggestion();
        if (s == null) {
            return "采纳建议标签，后续在档案管理页人工维护";
        }
        List<String> values = new ArrayList<>();
        Object cands = s.get("candidates");
        if (cands instanceof List<?> list) {
            for (Object c : list) {
                if (c instanceof Map<?, ?> m && m.get("suggestedValue") instanceof List<?> sv) {
                    for (Object v : sv) {
                        values.add(String.valueOf(v));
                    }
                }
            }
        }
        if (values.isEmpty() && s.get("suggestedTags") instanceof List<?> tags) {
            for (Object t : tags) {
                values.add(String.valueOf(t));
            }
        }
        if (values.isEmpty() && s.get("addTags") instanceof List<?> addTags) {
            for (Object t : addTags) {
                values.add(String.valueOf(t));
            }
        }
        if (values.isEmpty()) {
            return "采纳建议标签，后续在档案管理页人工维护";
        }
        return "建议补充标签：" + String.join("、", values) + "（可在档案管理页采纳）";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
