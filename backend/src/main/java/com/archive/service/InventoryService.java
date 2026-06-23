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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<InventoryTask> tasks = p.getRecords();
        Map<Long, String> roomNoMap = loadRoomNos(tasks.stream().map(InventoryTask::getRoomId).filter(java.util.Objects::nonNull).distinct().toList());
        Map<Integer, String> categoryNameMap = loadCategoryNames(tasks.stream().map(InventoryTask::getCategoryId).filter(java.util.Objects::nonNull).distinct().toList());
        Map<Long, long[]> taskStats = loadTaskStats(tasks.stream().map(InventoryTask::getId).toList());
        List<InventoryTaskResponse> recs = tasks.stream().map(t -> {
            InventoryTaskResponse r = toResponse(t);
            enrichTaskResponse(r, t, roomNoMap, categoryNameMap, taskStats);
            return r;
        }).toList();
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
        return toItemResponse(it, t);
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

    /** 18.5 删除草稿盘点任务（仅 draft 可删，级联清理应盘明细） */
    @Transactional
    public void deleteTask(Long taskId) {
        requireRole();
        InventoryTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点任务不存在");
        }
        if (task.getStatus() != InventoryTaskStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "仅草稿状态的盘点任务可删除");
        }
        // 草稿未实际盘点，级联清理应盘明细后物理删除任务
        itemMapper.delete(new QueryWrapper<InventoryItem>().eq("task_id", taskId));
        taskMapper.deleteById(taskId);
        auditService.log("M12", "delete_inventory_task", "inventory_task", taskId,
                Map.of("taskNo", task.getTaskNo() != null ? task.getTaskNo() : ""));
    }

    /** 18.4 获取盘点详情（任务 + 明细 + 统计） */
    public InventoryTaskDetailResponse getDetail(Long taskId) {
        requireRole();
        InventoryTask t = mustGet(taskId);
        List<InventoryItem> items = itemMapper.selectList(new QueryWrapper<InventoryItem>()
                .eq("task_id", taskId).isNull("deleted_at").orderByAsc("id"));
        InventoryTaskDetailResponse resp = new InventoryTaskDetailResponse();
        copy(t, resp);
        Map<Long, String> roomNoMap = loadRoomNos(List.of(t.getRoomId()));
        Map<Integer, String> categoryNameMap = loadCategoryNames(t.getCategoryId() != null ? List.of(t.getCategoryId()) : List.of());
        Map<Long, long[]> taskStats = loadTaskStats(List.of(taskId));
        enrichTaskResponse(resp, t, roomNoMap, categoryNameMap, taskStats);
        resp.setItems(items.stream().map(it -> toItemResponse(it, t)).toList());
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
        r.setCreatedAt(t.getCreatedAt());
    }

    /** 填充 roomNo/categoryName/total/checked/abnormalCount 等展示字段 */
    private void enrichTaskResponse(InventoryTaskResponse r, InventoryTask t,
                                    Map<Long, String> roomNoMap,
                                    Map<Integer, String> categoryNameMap,
                                    Map<Long, long[]> taskStats) {
        if (t.getRoomId() != null) r.setRoomNo(roomNoMap.get(t.getRoomId()));
        if (t.getCategoryId() != null) r.setCategoryName(categoryNameMap.get(t.getCategoryId()));
        long[] st = taskStats.get(t.getId());
        if (st != null) {
            // [total, abnormalCount]；明细创建时 check_result 默认 normal，故已核对数 = 应盘总数
            r.setTotal(st[0]);
            r.setChecked(st[0]);
            r.setAbnormalCount(st[1]);
        } else {
            r.setTotal(0L);
            r.setChecked(0L);
            r.setAbnormalCount(0L);
        }
    }

    /** 批量加载库房号 roomNo */
    private Map<Long, String> loadRoomNos(Collection<Long> roomIds) {
        if (roomIds.isEmpty()) return Map.of();
        String in = roomIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, room_no FROM warehouse_rooms WHERE id IN (" + in + ")");
        Map<Long, String> map = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("id")).longValue(), (String) row.get("room_no"));
        }
        return map;
    }

    /** 批量加载门类名称 categoryName */
    private Map<Integer, String> loadCategoryNames(Collection<Integer> categoryIds) {
        if (categoryIds.isEmpty()) return Map.of();
        String in = categoryIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, category_name FROM categories WHERE id IN (" + in + ")");
        Map<Integer, String> map = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("id")).intValue(), (String) row.get("category_name"));
        }
        return map;
    }

    /** 批量统计每个任务的 [total, abnormalCount]。
     *  明细创建时 check_result 默认 normal（NOT NULL），故 checked = total。
     *  abnormal = check_result IN (missing, misplaced, damaged)。 */
    private Map<Long, long[]> loadTaskStats(Collection<Long> taskIds) {
        if (taskIds.isEmpty()) return Map.of();
        String in = taskIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        // total: 全部明细；abnormal: checkResult in (missing, misplaced, damaged)
        List<Map<String, Object>> totalRows = jdbcTemplate.queryForList(
                "SELECT task_id AS tid, COUNT(*) AS cnt FROM inventory_items " +
                        "WHERE task_id IN (" + in + ") AND deleted_at IS NULL GROUP BY task_id");
        List<Map<String, Object>> abnormalRows = jdbcTemplate.queryForList(
                "SELECT task_id AS tid, COUNT(*) AS cnt FROM inventory_items " +
                        "WHERE task_id IN (" + in + ") AND deleted_at IS NULL " +
                        "AND check_result IN ('missing','misplaced','damaged') GROUP BY task_id");
        Map<Long, long[]> result = new java.util.HashMap<>();
        for (Long id : taskIds) result.put(id, new long[]{0L, 0L});
        for (Map<String, Object> row : totalRows) {
            result.get(((Number) row.get("tid")).longValue())[0] = ((Number) row.get("cnt")).longValue();
        }
        for (Map<String, Object> row : abnormalRows) {
            result.get(((Number) row.get("tid")).longValue())[1] = ((Number) row.get("cnt")).longValue();
        }
        return result;
    }

    private InventoryItemResponse toItemResponse(InventoryItem it, InventoryTask task) {
        InventoryItemResponse r = new InventoryItemResponse();
        r.setId(it.getId());
        r.setArchiveId(it.getArchiveId());
        r.setBoxId(it.getBoxId());
        r.setExpectedLocationId(it.getExpectedLocationId());
        r.setActualLocationCode(it.getActualLocationCode());
        r.setCheckResult(it.getCheckResult() != null ? it.getCheckResult().name() : null);
        r.setNote(it.getNote());
        // 填充展示字段：archiveNo/title/loanStatus（来自 archives），expectedLocationCode（来自 storage_locations）
        if (it.getArchiveId() != null) {
            List<Map<String, Object>> arcs = jdbcTemplate.queryForList(
                    "SELECT archive_no, title, loan_status FROM archives WHERE id = ?", it.getArchiveId());
            if (!arcs.isEmpty()) {
                Map<String, Object> a = arcs.get(0);
                r.setArchiveNo((String) a.get("archive_no"));
                r.setTitle((String) a.get("title"));
                Object loan = a.get("loan_status");
                r.setLoanStatus(loan != null ? loan.toString() : null);
            }
        }
        if (it.getExpectedLocationId() != null) {
            List<Map<String, Object>> locs = jdbcTemplate.queryForList(
                    "SELECT location_code FROM storage_locations WHERE id = ?", it.getExpectedLocationId());
            if (!locs.isEmpty()) {
                r.setExpectedLocationCode((String) locs.get(0).get("location_code"));
            }
        }
        return r;
    }
}
