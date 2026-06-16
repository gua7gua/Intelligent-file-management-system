package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.AppraisalBatchCreateRequest;
import com.archive.dto.request.AppraisalItemSaveRequest;
import com.archive.dto.response.AppraisalBatchDetailResponse;
import com.archive.dto.response.AppraisalBatchResponse;
import com.archive.entity.AppraisalBatch;
import com.archive.entity.AppraisalItem;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.AppraisalBatchStatus;
import com.archive.enums.AppraisalResult;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.RetentionPeriod;
import com.archive.exception.BusinessException;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.AppraisalItemMapper;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.util.AppraisalNoUtil;
import com.archive.util.DestructionNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 档案鉴定服务。
 * 负责鉴定批次创建/查询/明细保存/完成（完成时按 destroy 结论生成销毁清册）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppraisalService {

    private final AppraisalBatchMapper batchMapper;
    private final AppraisalItemMapper itemMapper;
    private final ArchiveMapper archiveMapper;
    private final DestructionListMapper destructionListMapper;
    private final DestructionItemMapper destructionItemMapper;
    private final ArchiveChangeLogMapper changeLogMapper;
    private final AppraisalNoUtil appraisalNoUtil;
    private final DestructionNoUtil destructionNoUtil;
    private final AuditService auditService;

    // ==================== 14.2 创建鉴定批次 ====================

    @Transactional
    public AppraisalBatchDetailResponse createBatch(AppraisalBatchCreateRequest req) {
        if (req.getFormedYearStart() != null && req.getFormedYearEnd() != null
                && req.getFormedYearStart() > req.getFormedYearEnd()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "年度起不能大于年度止");
        }

        AppraisalBatch batch = new AppraisalBatch();
        batch.setBatchNo(appraisalNoUtil.generate());
        batch.setBatchName(req.getBatchName());
        batch.setCategoryId(req.getCategoryId());
        batch.setFormedYearStart(req.getFormedYearStart());
        batch.setFormedYearEnd(req.getFormedYearEnd());
        batch.setStatus(AppraisalBatchStatus.draft);
        batchMapper.insert(batch);

        List<Archive> hits = findDueArchives(req.getCategoryId(),
                req.getFormedYearStart(), req.getFormedYearEnd());
        for (Archive a : hits) {
            AppraisalItem item = new AppraisalItem();
            item.setBatchId(batch.getId());
            item.setArchiveId(a.getId());
            itemMapper.insert(item);
        }

        auditService.log("M10", "create_appraisal_batch", "appraisal_batch", batch.getId(),
                Map.of("batchNo", batch.getBatchNo(), "hitCount", hits.size()));

        return toDetail(batch, hits);
    }

    /** 命中 retention_until 到期且未销毁的档案。 */
    private List<Archive> findDueArchives(Integer categoryId, Integer yearStart, Integer yearEnd) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.isNotNull("retention_until");
        w.apply("retention_until < CURRENT_DATE");
        w.ne("lifecycle_status", "destroyed");
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (yearStart != null) {
            w.ge("formed_year", yearStart);
        }
        if (yearEnd != null) {
            w.le("formed_year", yearEnd);
        }
        w.orderByAsc("id");
        return archiveMapper.selectList(w);
    }

    // ==================== 14.1 查询鉴定批次 ====================

    public PageResult<AppraisalBatchResponse> listBatches(
            String status, Integer categoryId, Integer yearStart, Integer yearEnd,
            int pageNo, int pageSize) {

        QueryWrapper<AppraisalBatch> w = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (yearStart != null) {
            w.ge("formed_year_start", yearStart);
        }
        if (yearEnd != null) {
            w.le("formed_year_end", yearEnd);
        }
        w.orderByDesc("id");

        Page<AppraisalBatch> page = batchMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<AppraisalBatchResponse> records = page.getRecords().stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private AppraisalBatchResponse toBatchResponse(AppraisalBatch b) {
        AppraisalBatchResponse r = new AppraisalBatchResponse();
        r.setId(b.getId());
        r.setBatchNo(b.getBatchNo());
        r.setBatchName(b.getBatchName());
        r.setCategoryId(b.getCategoryId());
        r.setFormedYearStart(b.getFormedYearStart());
        r.setFormedYearEnd(b.getFormedYearEnd());
        r.setStatus(b.getStatus() != null ? b.getStatus().name() : null);
        r.setCompletedAt(b.getCompletedAt());

        QueryWrapper<AppraisalItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", b.getId());
        Long c = itemMapper.selectCount(iw);
        r.setItemCount(c != null ? c.intValue() : 0);
        return r;
    }

    // ==================== 14.3 获取鉴定批次详情 ====================

    public AppraisalBatchDetailResponse getBatchDetail(Long batchId) {
        AppraisalBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "鉴定批次不存在");
        }

        QueryWrapper<AppraisalItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batchId);
        iw.orderByAsc("id");
        List<AppraisalItem> items = itemMapper.selectList(iw);

        List<Long> archiveIds = items.stream().map(AppraisalItem::getArchiveId)
                .collect(Collectors.toList());
        Map<Long, Archive> archiveMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                archiveMap.put(a.getId(), a);
            }
        }
        return toDetailFromItems(batch, items, archiveMap);
    }

    // ==================== 14.4 保存鉴定明细 ====================

    @Transactional
    public AppraisalBatchDetailResponse saveItems(Long batchId, AppraisalItemSaveRequest req) {
        AppraisalBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "鉴定批次不存在");
        }
        if (batch.getStatus() != AppraisalBatchStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非草稿批次不可修改鉴定明细");
        }

        // 校验
        Set<Long> seen = new HashSet<>();
        for (AppraisalItemSaveRequest.Item it : req.getItems()) {
            String result = it.getAppraisalResult();
            if (!"extend".equals(result) && !"destroy".equals(result)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "鉴定结论必须为 extend 或 destroy");
            }
            if ("extend".equals(result)
                    && (it.getNewRetentionPeriod() == null || it.getNewRetentionPeriod().isBlank())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "延长保管必须填写新保管期限");
            }
            if (!seen.add(it.getArchiveId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "鉴定明细档案重复");
            }
        }

        // 加载 extend 涉及档案（用于到期日推算）
        List<Long> extendArchiveIds = req.getItems().stream()
                .filter(it -> "extend".equals(it.getAppraisalResult()))
                .map(AppraisalItemSaveRequest.Item::getArchiveId)
                .collect(Collectors.toList());
        Map<Long, Archive> extendArchMap = new HashMap<>();
        if (!extendArchiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(extendArchiveIds)) {
                extendArchMap.put(a.getId(), a);
            }
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();
        for (AppraisalItemSaveRequest.Item it : req.getItems()) {
            AppraisalItem existing = itemMapper.selectOne(new QueryWrapper<AppraisalItem>()
                    .eq("batch_id", batchId).eq("archive_id", it.getArchiveId()));
            if (existing == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "档案不属于该鉴定批次");
            }
            existing.setAppraisalResult(AppraisalResult.valueOf(it.getAppraisalResult()));
            existing.setOpinion(it.getOpinion());
            if ("extend".equals(it.getAppraisalResult())) {
                existing.setNewRetentionPeriod(it.getNewRetentionPeriod());
                Archive ea = extendArchMap.get(it.getArchiveId());
                existing.setNewRetentionUntil(computeRetentionUntil(
                        ea != null ? ea.getFormedYear() : null, it.getNewRetentionPeriod()));
            }
            existing.setAppraisedBy(uid);
            existing.setAppraisedAt(now);
            itemMapper.updateById(existing);
        }

        auditService.log("M10", "save_appraisal_items", "appraisal_batch", batchId,
                Map.of("count", req.getItems().size()));

        return getBatchDetail(batchId);
    }

    // ==================== 14.5 完成鉴定 ====================

    @Transactional
    public AppraisalBatchDetailResponse completeBatch(Long batchId) {
        AppraisalBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "鉴定批次不存在");
        }
        if (batch.getStatus() != AppraisalBatchStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "非草稿批次不可完成鉴定");
        }

        QueryWrapper<AppraisalItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batchId);
        List<AppraisalItem> items = itemMapper.selectList(iw);
        for (AppraisalItem it : items) {
            if (it.getAppraisalResult() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "存在未填写鉴定结论的明细");
            }
        }

        List<Long> archiveIds = items.stream().map(AppraisalItem::getArchiveId).collect(Collectors.toList());
        Map<Long, Archive> archMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            for (Archive a : archiveMapper.selectBatchIds(archiveIds)) {
                archMap.put(a.getId(), a);
            }
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();
        List<AppraisalItem> destroyItems = new ArrayList<>();

        for (AppraisalItem it : items) {
            Archive a = archMap.get(it.getArchiveId());
            if (a == null) {
                continue;
            }
            if (it.getAppraisalResult() == AppraisalResult.extend) {
                RetentionPeriod oldPeriod = a.getRetentionPeriod();
                RetentionPeriod newPeriod = resolveRetentionPeriod(it.getNewRetentionPeriod());
                // 延期为"永久"时 newRetentionUntil 为 null。MyBatis-Plus updateById 默认跳过 null 字段，
                // 会令 retention_until 残留旧值，违反 ck_archives_retention_until（永久须 retention_until IS NULL）。
                // 故用 UpdateWrapper 显式 set 两个字段（.set 始终写入，含 null）。
                UpdateWrapper<Archive> uw = new UpdateWrapper<>();
                uw.eq("id", a.getId())
                        .set("retention_period", newPeriod != null ? newPeriod.getDbValue() : null)
                        .set("retention_until", it.getNewRetentionUntil());
                archiveMapper.update(null, uw);
                a.setRetentionPeriod(newPeriod);
                a.setRetentionUntil(it.getNewRetentionUntil());
                writeChangeLog(a.getId(), "retention_period",
                        oldPeriod != null ? oldPeriod.getDbValue() : null,
                        a.getRetentionPeriod() != null ? a.getRetentionPeriod().getDbValue() : null,
                        it.getOpinion(), "appraisal", null, uid, now);
            } else {
                a.setLifecycleStatus(com.archive.enums.LifecycleStatus.pending_destruction);
                archiveMapper.updateById(a);
                destroyItems.add(it);
            }
        }

        // 生成销毁清册（若有销毁结论）
        if (!destroyItems.isEmpty()) {
            DestructionList list = new DestructionList();
            list.setListNo(destructionNoUtil.generate());
            list.setListName(batch.getBatchName() + " 销毁清册");
            list.setAppraisalBatchId(batchId);
            list.setStatus(DestructionListStatus.draft);
            destructionListMapper.insert(list);

            for (AppraisalItem it : destroyItems) {
                Archive a = archMap.get(it.getArchiveId());
                DestructionItem di = new DestructionItem();
                di.setDestructionListId(list.getId());
                di.setArchiveId(a.getId());
                di.setArchiveNoSnapshot(a.getArchiveNo());
                di.setTitleSnapshot(a.getTitle());
                di.setCategorySnapshot(a.getCategoryId() != null ? String.valueOf(a.getCategoryId()) : null);
                di.setRetentionSnapshot(a.getRetentionPeriod() != null ? a.getRetentionPeriod().getDbValue() : null);
                di.setSecurityLevelSnapshot(a.getSecurityLevel());
                di.setAppraisalOpinionSnapshot(it.getOpinion());
                di.setFileDeleteStatus("not_started");
                destructionItemMapper.insert(di);
            }
        }

        batch.setStatus(AppraisalBatchStatus.completed);
        batch.setCompletedAt(now);
        batchMapper.updateById(batch);

        auditService.log("M10", "complete_appraisal", "appraisal_batch", batchId,
                Map.of("destroyCount", destroyItems.size()));

        return getBatchDetail(batchId);
    }

    // ==================== 辅助方法 ====================

    /** 由命中档案列表组装批次详情（明细 appraisalResult 暂为 null）。 */
    private AppraisalBatchDetailResponse toDetail(AppraisalBatch batch, List<Archive> archives) {
        AppraisalBatchDetailResponse resp = baseDetail(batch);
        List<AppraisalBatchDetailResponse.ItemView> items = new ArrayList<>();
        for (Archive a : archives) {
            AppraisalBatchDetailResponse.ItemView v = archiveView(a);
            items.add(v);
        }
        resp.setItems(items);
        return resp;
    }

    /** 由已保存明细 + 档案组装详情（含鉴定结论）。 */
    private AppraisalBatchDetailResponse toDetailFromItems(
            AppraisalBatch batch, List<AppraisalItem> items, Map<Long, Archive> archiveMap) {
        AppraisalBatchDetailResponse resp = baseDetail(batch);
        List<AppraisalBatchDetailResponse.ItemView> views = new ArrayList<>();
        for (AppraisalItem it : items) {
            AppraisalBatchDetailResponse.ItemView v = new AppraisalBatchDetailResponse.ItemView();
            v.setArchiveId(it.getArchiveId());
            v.setAppraisalResult(it.getAppraisalResult() != null ? it.getAppraisalResult().name() : null);
            v.setNewRetentionPeriod(it.getNewRetentionPeriod());
            v.setOpinion(it.getOpinion());
            Archive a = archiveMap.get(it.getArchiveId());
            if (a != null) {
                v.setArchiveNo(a.getArchiveNo());
                v.setTitle(a.getTitle());
                v.setFormedYear(a.getFormedYear());
                v.setRetentionPeriod(a.getRetentionPeriod() != null ? a.getRetentionPeriod().getDbValue() : null);
                v.setRetentionUntil(a.getRetentionUntil());
            }
            views.add(v);
        }
        resp.setItems(views);
        return resp;
    }

    private AppraisalBatchDetailResponse baseDetail(AppraisalBatch batch) {
        AppraisalBatchDetailResponse resp = new AppraisalBatchDetailResponse();
        resp.setId(batch.getId());
        resp.setBatchNo(batch.getBatchNo());
        resp.setBatchName(batch.getBatchName());
        resp.setCategoryId(batch.getCategoryId());
        resp.setFormedYearStart(batch.getFormedYearStart());
        resp.setFormedYearEnd(batch.getFormedYearEnd());
        resp.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
        resp.setCompletedAt(batch.getCompletedAt());
        return resp;
    }

    private AppraisalBatchDetailResponse.ItemView archiveView(Archive a) {
        AppraisalBatchDetailResponse.ItemView v = new AppraisalBatchDetailResponse.ItemView();
        v.setArchiveId(a.getId());
        v.setArchiveNo(a.getArchiveNo());
        v.setTitle(a.getTitle());
        v.setFormedYear(a.getFormedYear());
        v.setRetentionPeriod(a.getRetentionPeriod() != null ? a.getRetentionPeriod().getDbValue() : null);
        v.setRetentionUntil(a.getRetentionUntil());
        return v;
    }

    /** 由形成年度与保管期限推算到期日；permanent 或无年度返回 null。 */
    private LocalDate computeRetentionUntil(Integer formedYear, String period) {
        if (formedYear == null || period == null || "permanent".equals(period)) {
            return null;
        }
        String digits = period.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        int years = Integer.parseInt(digits);
        return LocalDate.of(formedYear + years, 12, 31);
    }

    /** 按 dbValue（10y/30y/permanent）解析保管期限枚举。 */
    private RetentionPeriod resolveRetentionPeriod(String dbValue) {
        for (RetentionPeriod rp : RetentionPeriod.values()) {
            if (rp.getDbValue().equals(dbValue)) {
                return rp;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "未知保管期限: " + dbValue);
    }

    private void writeChangeLog(Long archiveId, String fieldName, String oldValue, String newValue,
                                String reason, String source, Long approvalRequestId,
                                Long changedBy, OffsetDateTime changedAt) {
        ArchiveChangeLog logEntry = new ArchiveChangeLog();
        logEntry.setArchiveId(archiveId);
        logEntry.setFieldName(fieldName);
        logEntry.setOldValue(oldValue);
        logEntry.setNewValue(newValue);
        logEntry.setChangeReason(reason);
        logEntry.setChangeSource(source);
        logEntry.setApprovalRequestId(approvalRequestId);
        logEntry.setChangedBy(changedBy);
        logEntry.setChangedAt(changedAt);
        changeLogMapper.insert(logEntry);
    }

    private Long safeCurrentUserId() {
        try {
            return AuthContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
