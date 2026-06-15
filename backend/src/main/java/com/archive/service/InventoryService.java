package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.InventoryItemUpdateRequest;
import com.archive.dto.request.InventoryTaskCreateRequest;
import com.archive.dto.request.InventoryTaskQuery;
import com.archive.dto.response.InventoryItemResponse;
import com.archive.dto.response.InventoryTaskDetailResponse;
import com.archive.dto.response.InventoryTaskResponse;
import com.archive.dto.response.InventoryTaskStatsResponse;
import com.archive.entity.InventoryItem;
import com.archive.entity.InventoryTask;
import com.archive.enums.InventoryCheckResult;
import com.archive.enums.InventoryTaskStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.InventoryItemMapper;
import com.archive.mapper.InventoryTaskMapper;
import com.archive.util.InventoryTaskNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 档案盘点服务（M12）。
 * 按库房 + 门类创建盘点任务并生成应盘明细，逐项核对，生成差异统计。
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryTaskMapper taskMapper;
    private final InventoryItemMapper itemMapper;
    private final JdbcTemplate jdbcTemplate;
    private final InventoryTaskNoUtil noUtil;
    private final AuditService auditService;

    /** 应盘候选档案：库房 room + 门类 category 内、未销毁的档案，带盒位与架位。 */
    private static final String CANDIDATE_SQL =
            "SELECT a.id AS archive_id, abi.box_id AS box_id, ab.location_id AS location_id, " +
            "a.loan_status AS loan_status FROM archive_box_items abi " +
            "JOIN archive_boxes ab ON ab.id = abi.box_id AND ab.deleted_at IS NULL " +
            "JOIN storage_locations sl ON sl.id = ab.location_id AND sl.deleted_at IS NULL " +
            "JOIN archives a ON a.id = abi.archive_id AND a.deleted_at IS NULL " +
            "WHERE abi.deleted_at IS NULL AND sl.room_id = ? AND a.category_id = ? " +
            "AND a.lifecycle_status <> 'destroyed' ORDER BY a.archive_no";

    private void requireRole() {
        if (!AuthContext.hasRole(RoleCode.back_archivist)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /** 18.1 查询盘点任务 */
    public PageResult<InventoryTaskResponse> list(InventoryTaskQuery q) {
        requireRole();
        QueryWrapper<InventoryTask> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (q.getStatus() != null && !q.getStatus().isBlank()) w.eq("status", q.getStatus());
        if (q.getRoomId() != null) w.eq("room_id", q.getRoomId());
        if (q.getCategoryId() != null) w.eq("category_id", q.getCategoryId());
        w.orderByDesc("created_at");
        Page<InventoryTask> p = taskMapper.selectPage(new Page<>(q.getPageNo(), q.getPageSize()), w);
        List<InventoryTaskResponse> recs = p.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(recs, q.getPageNo(), q.getPageSize(), p.getTotal());
    }

    /** 18.2 创建任务并生成应盘明细 */
    @Transactional
    public InventoryTaskDetailResponse create(InventoryTaskCreateRequest req) {
        requireRole();
        InventoryTask t = new InventoryTask();
        t.setTaskNo(noUtil.generate());
        t.setTaskName(req.getTaskName());
        t.setRoomId(req.getRoomId());
        t.setCategoryId(req.getCategoryId());
        t.setStatus(InventoryTaskStatus.draft);
        taskMapper.insert(t);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(CANDIDATE_SQL, req.getRoomId(), req.getCategoryId());
        for (Map<String, Object> r : rows) {
            InventoryItem it = new InventoryItem();
            it.setTaskId(t.getId());
            it.setArchiveId(((Number) r.get("archive_id")).longValue());
            it.setBoxId(r.get("box_id") != null ? ((Number) r.get("box_id")).longValue() : null);
            it.setExpectedLocationId(r.get("location_id") != null ? ((Number) r.get("location_id")).longValue() : null);
            String loan = r.get("loan_status") != null ? r.get("loan_status").toString() : null;
            // 当前已借出(loan_status=on_loan) 的档案标记为「借出中」，否则默认正常待核验
            it.setCheckResult("on_loan".equals(loan)
                    ? InventoryCheckResult.on_loan : InventoryCheckResult.normal);
            itemMapper.insert(it);
        }
        auditService.log("M12", "create_inventory_task", "inventory_task", t.getId(),
                Map.of("room", req.getRoomId(), "category", req.getCategoryId(), "items", rows.size()));
        return getDetail(t.getId());
    }

    /** 18.3 开始盘点 */
    @Transactional
    public InventoryTaskDetailResponse start(Long taskId) {
        requireRole();
        InventoryTask t = mustGet(taskId);
        if (t.getStatus() != InventoryTaskStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "任务状态不允许开始");
        }
        Long cnt = itemMapper.selectCount(new QueryWrapper<InventoryItem>()
                .eq("task_id", taskId).isNull("deleted_at"));
        if (cnt == null || cnt == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "任务没有盘点明细，无法开始");
        }
        t.setStatus(InventoryTaskStatus.running);
        t.setStartedAt(OffsetDateTime.now());
        taskMapper.updateById(t);
        auditService.log("M12", "start_inventory", "inventory_task", taskId, Map.of());
        return getDetail(taskId);
    }

    /** 18.5 更新盘点明细 */
    @Transactional
    public InventoryItemResponse updateItem(Long taskId, Long itemId, InventoryItemUpdateRequest req) {
        requireRole();
        InventoryTask t = mustGet(taskId);
        if (t.getStatus() != InventoryTaskStatus.running) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "任务不在盘点中，无法更新明细");
        }
        InventoryItem it = itemMapper.selectById(itemId);
        if (it == null || !taskId.equals(it.getTaskId()) || it.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点明细不存在");
        }
        it.setActualLocationCode(req.getActualLocationCode());
        it.setCheckResult(req.getCheckResult());
        it.setNote(req.getNote());
        itemMapper.updateById(it);
        return toItemResponse(it);
    }

    /** 18.6 完成盘点 */
    @Transactional
    public InventoryTaskDetailResponse complete(Long taskId, String summary) {
        requireRole();
        InventoryTask t = mustGet(taskId);
        if (t.getStatus() != InventoryTaskStatus.running) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "任务不在盘点中，无法完成");
        }
        t.setStatus(InventoryTaskStatus.completed);
        t.setCompletedAt(OffsetDateTime.now());
        t.setSummary(summary);
        taskMapper.updateById(t);
        auditService.log("M12", "complete_inventory", "inventory_task", taskId, Map.of());
        return getDetail(taskId);
    }

    /** 18.4 获取盘点详情（任务 + 明细 + 统计） */
    public InventoryTaskDetailResponse getDetail(Long taskId) {
        requireRole();
        InventoryTask t = mustGet(taskId);
        List<InventoryItem> items = itemMapper.selectList(new QueryWrapper<InventoryItem>()
                .eq("task_id", taskId).isNull("deleted_at").orderByAsc("id"));
        InventoryTaskDetailResponse resp = new InventoryTaskDetailResponse();
        copy(t, resp);
        resp.setItems(items.stream().map(this::toItemResponse).toList());
        resp.setStats(statsOf(items));
        return resp;
    }

    private InventoryTaskStatsResponse statsOf(List<InventoryItem> items) {
        InventoryTaskStatsResponse s = new InventoryTaskStatsResponse();
        for (InventoryItem it : items) {
            if (it.getCheckResult() == null) continue;
            switch (it.getCheckResult()) {
                case normal -> s.setNormal(s.getNormal() + 1);
                case missing -> s.setMissing(s.getMissing() + 1);
                case misplaced -> s.setMisplaced(s.getMisplaced() + 1);
                case damaged -> s.setDamaged(s.getDamaged() + 1);
                case on_loan -> s.setOnLoan(s.getOnLoan() + 1);
            }
        }
        return s;
    }

    private InventoryTask mustGet(Long taskId) {
        InventoryTask t = taskMapper.selectById(taskId);
        if (t == null || t.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点任务不存在");
        }
        return t;
    }

    private InventoryTaskResponse toResponse(InventoryTask t) {
        InventoryTaskResponse r = new InventoryTaskResponse();
        copy(t, r);
        return r;
    }

    private void copy(InventoryTask t, InventoryTaskResponse r) {
        r.setId(t.getId());
        r.setTaskNo(t.getTaskNo());
        r.setTaskName(t.getTaskName());
        r.setRoomId(t.getRoomId());
        r.setCategoryId(t.getCategoryId());
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setStartedAt(t.getStartedAt());
        r.setCompletedAt(t.getCompletedAt());
        r.setSummary(t.getSummary());
    }

    private InventoryItemResponse toItemResponse(InventoryItem it) {
        InventoryItemResponse r = new InventoryItemResponse();
        r.setId(it.getId());
        r.setArchiveId(it.getArchiveId());
        r.setBoxId(it.getBoxId());
        r.setExpectedLocationId(it.getExpectedLocationId());
        r.setActualLocationCode(it.getActualLocationCode());
        r.setCheckResult(it.getCheckResult() != null ? it.getCheckResult().name() : null);
        r.setNote(it.getNote());
        return r;
    }
}
