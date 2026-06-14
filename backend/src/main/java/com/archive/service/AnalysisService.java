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
import com.archive.enums.LifecycleStatus;
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
        t.setTaskType(req.getTaskType());
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

        runRuleScan(t.getId(), req.getTaskType(), archiveIds);

        boolean wantAi = Boolean.TRUE.equals(rule.getIncludeAiSuggestion());
        if (wantAi && aiTaskService.aiAvailable()) {
            Long aiTaskId = aiTaskService.startArchiveAnalysis(t.getId(), archiveIds);
            t.setLatestAiTaskId(aiTaskId);
            t.setStatus(AnalysisTaskStatus.running);
        } else {
            t.setStatus(AnalysisTaskStatus.completed);
            t.setCompletedAt(OffsetDateTime.now());
        }
        taskMapper.updateById(t);
        auditService.log("M14", "create_analysis_task", "analysis_task", t.getId(),
                Map.of("taskType", req.getTaskType().name(), "archives", archiveIds.size()));
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
                "SELECT id FROM archives WHERE deleted_at IS NULL AND lifecycle_status<>'destroyed'");
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
        resp.setItems(items.stream().map(this::toItemResponse).toList());
        return resp;
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
        it.setStatus("adopted".equals(req.getAction()) ? AnalysisItemStatus.adopted : AnalysisItemStatus.rejected);
        it.setHandledBy(AuthContext.getCurrentUserId());
        it.setHandledAt(OffsetDateTime.now());
        itemMapper.updateById(it);
        auditService.log("M14", "handle_analysis_item", "analysis_item", itemId,
                Map.of("action", req.getAction()));
        return toItemResponse(it);
    }

    private void copy(AnalysisTask t, AnalysisTaskResponse r) {
        r.setId(t.getId());
        r.setTaskNo(t.getTaskNo());
        r.setTaskType(t.getTaskType() != null ? t.getTaskType().name() : null);
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setLatestAiTaskId(t.getLatestAiTaskId());
        r.setStartedAt(t.getStartedAt());
        r.setCompletedAt(t.getCompletedAt());
    }

    private AnalysisTaskResponse toResponse(AnalysisTask t) {
        AnalysisTaskResponse r = new AnalysisTaskResponse();
        copy(t, r);
        return r;
    }

    private AnalysisItemResponse toItemResponse(AnalysisItem it) {
        AnalysisItemResponse r = new AnalysisItemResponse();
        r.setId(it.getId());
        r.setTaskId(it.getTaskId());
        r.setArchiveId(it.getArchiveId());
        r.setIssueType(it.getIssueType() != null ? it.getIssueType().name() : null);
        r.setIssueDetail(it.getIssueDetail());
        r.setSuggestion(it.getSuggestion());
        r.setStatus(it.getStatus() != null ? it.getStatus().name() : null);
        r.setHandledAt(it.getHandledAt());
        return r;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
