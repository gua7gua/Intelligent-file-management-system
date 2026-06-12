package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.*;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.IntakeItemResponse;
import com.archive.dto.response.TransferDashboardResponse;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.enums.*;
import com.archive.exception.BusinessException;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 移交/征集清单核心业务。
 */
@Service
@RequiredArgsConstructor
public class IntakeBatchService {

    private final IntakeBatchMapper batchMapper;
    private final IntakeItemMapper itemMapper;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 序列号 ====================

    private String generateBatchNo() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_batch_no')", Long.class);
        return String.format("BAT-%06d", seq);
    }

    // ==================== 移交清单 CRUD ====================

    /**
     * 创建移交清单草稿。
     */
    @Transactional
    public IntakeBatchResponse createTransferBatch(TransferBatchCreateRequest req) {
        long userId = AuthContext.getCurrentUserId();
        Long orgId = AuthContext.getOrganizationId();
        if (orgId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无所属单位，无法创建移交清单");
        }

        IntakeBatch batch = new IntakeBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setSourceType(SourceType.transfer);
        batch.setTitle(req.getTitle());
        batch.setStatus(BatchStatus.draft);
        batch.setOrganizationId(orgId);
        batch.setDepartmentName(req.getDepartmentName());
        batch.setContactName(AuthContext.isAuthenticated()
                ? getUserName(userId) : "");
        batch.setContactPhone(req.getContactPhone());
        batch.setArchiveYear(req.getArchiveYear());
        batch.setExpectedTransferDate(req.getExpectedTransferDate());
        batchMapper.insert(batch);

        int itemNo = 0;
        for (TransferItemRequest itemReq : req.getItems()) {
            itemNo++;
            insertTransferItem(batch.getId(), itemNo, itemReq);
        }

        return getTransferBatch(batch.getId());
    }

    /**
     * 获取移交清单详情。
     */
    public IntakeBatchResponse getTransferBatch(Long batchId) {
        IntakeBatch batch = getBatchAndCheckOwner(batchId, SourceType.transfer);
        return toBatchResponse(batch, true);
    }

    /**
     * 更新移交清单草稿。
     */
    @Transactional
    public IntakeBatchResponse updateTransferBatch(Long batchId, TransferBatchCreateRequest req) {
        IntakeBatch batch = getBatchAndCheckOwner(batchId, SourceType.transfer);
        checkDraftStatus(batch);

        batch.setTitle(req.getTitle());
        batch.setDepartmentName(req.getDepartmentName());
        batch.setContactPhone(req.getContactPhone());
        batch.setArchiveYear(req.getArchiveYear());
        batch.setExpectedTransferDate(req.getExpectedTransferDate());
        batchMapper.updateById(batch);

        // 删除旧条目并重新插入
        itemMapper.delete(new QueryWrapper<IntakeItem>().eq("batch_id", batchId));
        int itemNo = 0;
        for (TransferItemRequest itemReq : req.getItems()) {
            itemNo++;
            insertTransferItem(batchId, itemNo, itemReq);
        }

        return getTransferBatch(batchId);
    }

    /**
     * 删除移交清单草稿。
     */
    @Transactional
    public void deleteTransferBatch(Long batchId) {
        IntakeBatch batch = getBatchAndCheckOwner(batchId, SourceType.transfer);
        checkDraftStatus(batch);

        itemMapper.delete(new QueryWrapper<IntakeItem>().eq("batch_id", batchId));
        batchMapper.deleteById(batchId);
    }

    /**
     * 分页查询移交清单。
     */
    public PageResult<IntakeBatchResponse> listTransferBatches(BatchPageQuery query) {
        Long orgId = AuthContext.getOrganizationId();

        Page<IntakeBatch> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<IntakeBatch> qw = new QueryWrapper<>();
        qw.eq("source_type", SourceType.transfer.name());
        qw.eq("organization_id", orgId);
        qw.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(w -> w.like("batch_no", query.getKeyword())
                    .or().like("title", query.getKeyword()));
        }
        if (query.getArchiveYear() != null) {
            qw.eq("archive_year", query.getArchiveYear());
        }
        qw.orderByDesc("created_at");

        Page<IntakeBatch> result = batchMapper.selectPage(page, qw);
        List<IntakeBatchResponse> voList = result.getRecords().stream()
                .map(b -> toBatchResponse(b, false))
                .collect(Collectors.toList());
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /**
     * 提交移交清单。
     */
    @Transactional
    public IntakeBatchResponse submitTransferBatch(Long batchId) {
        IntakeBatch batch = getBatchAndCheckOwner(batchId, SourceType.transfer);
        checkDraftStatus(batch);

        // 校验条目完整性
        List<IntakeItem> items = itemMapper.selectList(
                new QueryWrapper<IntakeItem>().eq("batch_id", batchId));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少包含一条清单条目");
        }

        // 更新条目状态
        for (IntakeItem item : items) {
            item.setStatus(ItemStatus.pending_acceptance);
            itemMapper.updateById(item);
        }

        batch.setStatus(BatchStatus.pending_transfer);
        batch.setSubmittedAt(OffsetDateTime.now());
        batchMapper.updateById(batch);

        return getTransferBatch(batchId);
    }

    // ==================== 移交清单条目 ====================

    /**
     * 新增移交清单条目。
     */
    @Transactional
    public IntakeItemResponse addTransferItem(Long batchId, TransferItemRequest req) {
        IntakeBatch batch = getBatchAndCheckOwner(batchId, SourceType.transfer);
        checkDraftStatus(batch);

        int maxNo = itemMapper.maxItemNo(batchId);
        IntakeItem item = buildTransferItem(batchId, maxNo + 1, req);
        itemMapper.insert(item);

        return toItemResponse(item);
    }

    /**
     * 更新移交清单条目。
     */
    @Transactional
    public IntakeItemResponse updateTransferItem(Long batchId, Long itemId, TransferItemRequest req) {
        getBatchAndCheckOwner(batchId, SourceType.transfer);
        IntakeItem item = getItemAndCheckBatch(itemId, batchId);
        if (item.getStatus() != ItemStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有草稿状态的条目可以编辑");
        }

        fillTransferItem(item, req);
        itemMapper.updateById(item);
        return toItemResponse(item);
    }

    /**
     * 删除移交清单条目。
     */
    @Transactional
    public void deleteTransferItem(Long batchId, Long itemId) {
        IntakeBatch batch = getBatchAndCheckOwner(batchId, SourceType.transfer);
        checkDraftStatus(batch);

        List<IntakeItem> remaining = itemMapper.selectList(
                new QueryWrapper<IntakeItem>().eq("batch_id", batchId).ne("id", itemId));
        if (remaining.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少保留一条条目");
        }

        itemMapper.deleteById(itemId);
    }

    // ==================== 移交工作台 ====================

    /**
     * 移交工作台统计。
     */
    public TransferDashboardResponse transferDashboard() {
        Long orgId = AuthContext.getOrganizationId();

        TransferDashboardResponse resp = new TransferDashboardResponse();
        TransferDashboardResponse.Summary summary = new TransferDashboardResponse.Summary();

        QueryWrapper<IntakeBatch> baseQw = new QueryWrapper<>();
        baseQw.eq("source_type", SourceType.transfer.name());
        baseQw.eq("organization_id", orgId);
        baseQw.isNull("deleted_at");

        for (BatchStatus bs : BatchStatus.values()) {
            long count = batchMapper.selectCount(
                    new QueryWrapper<IntakeBatch>().eq("source_type", SourceType.transfer.name())
                            .eq("organization_id", orgId).eq("status", bs.name()).isNull("deleted_at"));
            switch (bs) {
                case draft -> summary.setDraft(count);
                case pending_transfer -> summary.setPendingTransfer(count);
                case partially_received -> summary.setPartiallyReceived(count);
                case received -> summary.setReceived(count);
                case archived -> summary.setArchived(count);
                case shelved -> summary.setShelved(count);
                case rejected -> summary.setRejected(count);
                default -> { /* 其他状态不统计 */ }
            }
        }

        resp.setSummary(summary);

        // 最近 5 条清单
        Page<IntakeBatch> recentPage = new Page<>(1, 5);
        QueryWrapper<IntakeBatch> recentQw = new QueryWrapper<>();
        recentQw.eq("source_type", SourceType.transfer.name())
                .eq("organization_id", orgId).isNull("deleted_at")
                .orderByDesc("created_at");
        List<IntakeBatchResponse> recent = batchMapper.selectPage(recentPage, recentQw)
                .getRecords().stream().map(b -> toBatchResponse(b, false)).collect(Collectors.toList());
        resp.setRecentBatches(recent);

        return resp;
    }

    // ==================== 征集清单 ====================

    /**
     * 创建征集清单草稿。
     */
    @Transactional
    public IntakeBatchResponse createCollection(CollectionBatchCreateRequest req) {
        long userId = AuthContext.getCurrentUserId();

        IntakeBatch batch = new IntakeBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setSourceType(SourceType.collection);
        batch.setTitle(req.getTitle());
        batch.setStatus(BatchStatus.draft);
        batch.setPublicUserId(userId);
        batch.setContactName(req.getContactName());
        batch.setContactPhone(req.getContactPhone());
        batch.setArchiveYear(req.getArchiveYear());
        batchMapper.insert(batch);

        int itemNo = 0;
        for (CollectionBatchCreateRequest.CollectionItemRequest itemReq : req.getItems()) {
            itemNo++;
            IntakeItem item = new IntakeItem();
            item.setBatchId(batch.getId());
            item.setItemNo(itemNo);
            item.setStatus(ItemStatus.draft);
            item.setInputTitle(itemReq.getInputTitle());
            item.setRetentionPeriod(RetentionPeriod.permanent);
            item.setCarrierStatus(CarrierStatus.valueOf(itemReq.getCarrierStatus()));
            item.setSecurityLevel(0);
            item.setOpenStatus("open");
            item.setAllowDigitization(false);
            item.setElectronicFormat(itemReq.getElectronicFormat());
            item.setExpectedFilename(itemReq.getExpectedFilename());
            item.setFormedDate(itemReq.getFormedDate());
            item.setFileMatchStatus(FileMatchStatus.none);
            itemMapper.insert(item);
        }

        return getCollection(batch.getId());
    }

    /**
     * 获取征集清单详情。
     */
    public IntakeBatchResponse getCollection(Long batchId) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getSourceType() != SourceType.collection) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "征集清单不存在");
        }
        long userId = AuthContext.getCurrentUserId();
        if (!batch.getPublicUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看本人的征集清单");
        }
        return toBatchResponse(batch, true);
    }

    /**
     * 更新征集清单草稿。
     */
    @Transactional
    public IntakeBatchResponse updateCollection(Long batchId, CollectionBatchCreateRequest req) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getSourceType() != SourceType.collection) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "征集清单不存在");
        }
        long userId = AuthContext.getCurrentUserId();
        if (!batch.getPublicUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能编辑本人的征集清单");
        }
        checkDraftStatus(batch);

        batch.setTitle(req.getTitle());
        batch.setContactName(req.getContactName());
        batch.setContactPhone(req.getContactPhone());
        batch.setArchiveYear(req.getArchiveYear());
        batchMapper.updateById(batch);

        itemMapper.delete(new QueryWrapper<IntakeItem>().eq("batch_id", batchId));
        int itemNo = 0;
        for (CollectionBatchCreateRequest.CollectionItemRequest itemReq : req.getItems()) {
            itemNo++;
            IntakeItem item = new IntakeItem();
            item.setBatchId(batchId);
            item.setItemNo(itemNo);
            item.setStatus(ItemStatus.draft);
            item.setInputTitle(itemReq.getInputTitle());
            item.setRetentionPeriod(RetentionPeriod.permanent);
            item.setCarrierStatus(CarrierStatus.valueOf(itemReq.getCarrierStatus()));
            item.setSecurityLevel(0);
            item.setOpenStatus("open");
            item.setAllowDigitization(false);
            item.setElectronicFormat(itemReq.getElectronicFormat());
            item.setExpectedFilename(itemReq.getExpectedFilename());
            item.setFormedDate(itemReq.getFormedDate());
            item.setFileMatchStatus(FileMatchStatus.none);
            itemMapper.insert(item);
        }

        return getCollection(batchId);
    }

    /**
     * 提交征集清单。
     */
    @Transactional
    public IntakeBatchResponse submitCollection(Long batchId, CollectionSubmitRequest req) {
        if (req.getAgreementAccepted() == null || !req.getAgreementAccepted()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "必须同意捐赠协议");
        }

        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getSourceType() != SourceType.collection) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "征集清单不存在");
        }
        long userId = AuthContext.getCurrentUserId();
        if (!batch.getPublicUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能提交本人的征集清单");
        }
        checkDraftStatus(batch);

        List<IntakeItem> items = itemMapper.selectList(
                new QueryWrapper<IntakeItem>().eq("batch_id", batchId));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少包含一条清单条目");
        }

        for (IntakeItem item : items) {
            item.setStatus(ItemStatus.pending_acceptance);
            itemMapper.updateById(item);
        }

        batch.setStatus(BatchStatus.pending_contact);
        batch.setSubmittedAt(OffsetDateTime.now());
        batch.setAgreementAcceptedAt(OffsetDateTime.now());
        batchMapper.updateById(batch);

        return getCollection(batchId);
    }

    /**
     * 分页查询公众征集清单。
     */
    public PageResult<IntakeBatchResponse> listCollections(BatchPageQuery query) {
        long userId = AuthContext.getCurrentUserId();

        Page<IntakeBatch> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<IntakeBatch> qw = new QueryWrapper<>();
        qw.eq("source_type", SourceType.collection.name());
        qw.eq("public_user_id", userId);
        qw.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(w -> w.like("batch_no", query.getKeyword())
                    .or().like("title", query.getKeyword()));
        }
        qw.orderByDesc("created_at");

        Page<IntakeBatch> result = batchMapper.selectPage(page, qw);
        List<IntakeBatchResponse> voList = result.getRecords().stream()
                .map(b -> toBatchResponse(b, false))
                .collect(Collectors.toList());
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    // ==================== 前台验收 ====================

    /**
     * 查询待验收批次。
     */
    public PageResult<IntakeBatchResponse> listPendingReception(BatchPageQuery query) {
        Page<IntakeBatch> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<IntakeBatch> qw = new QueryWrapper<>();
        qw.isNull("deleted_at");
        qw.in("status",
                BatchStatus.pending_transfer.name(),
                BatchStatus.pending_receive.name(),
                BatchStatus.received.name(),
                BatchStatus.partially_received.name());
        if (query.getSourceType() != null && !query.getSourceType().isBlank()) {
            qw.eq("source_type", query.getSourceType());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(w -> w.like("batch_no", query.getKeyword())
                    .or().like("title", query.getKeyword())
                    .or().like("contact_name", query.getKeyword()));
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        qw.orderByAsc("submitted_at");

        Page<IntakeBatch> result = batchMapper.selectPage(page, qw);
        List<IntakeBatchResponse> voList = result.getRecords().stream()
                .map(b -> toBatchResponse(b, false))
                .collect(Collectors.toList());
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /**
     * 获取验收详情。
     */
    public IntakeBatchResponse getReceptionDetail(Long batchId) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单不存在");
        }
        return toBatchResponse(batch, true);
    }

    /**
     * 条目验收/回退。
     */
    @Transactional
    public IntakeItemResponse acceptItem(Long itemId, ItemAcceptanceRequest req) {
        IntakeItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "条目不存在");
        }
        if (item.getStatus() != ItemStatus.pending_acceptance) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有待验收状态的条目可以操作");
        }

        if ("accepted".equals(req.getResult())) {
            item.setStatus(ItemStatus.accepted);
            item.setAcceptanceNote(req.getAcceptanceNote());
        } else if ("rejected".equals(req.getResult())) {
            if (req.getRejectReason() == null || req.getRejectReason().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "回退时必须填写回退原因");
            }
            item.setStatus(ItemStatus.rejected);
            item.setRejectReason(req.getRejectReason());
            item.setAcceptanceNote(req.getAcceptanceNote());
            // 注意：暂存文件删除由刘星 feat/file-liu 实现
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验收结果只能为 accepted 或 rejected");
        }

        itemMapper.updateById(item);
        return toItemResponse(item);
    }

    /**
     * 完成批次验收。
     */
    @Transactional
    public IntakeBatchResponse completeAcceptance(Long batchId, BatchCompleteRequest req) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单不存在");
        }

        // 校验所有条目已有结论
        List<IntakeItem> items = itemMapper.selectList(
                new QueryWrapper<IntakeItem>().eq("batch_id", batchId));
        boolean allDecided = items.stream()
                .allMatch(i -> i.getStatus() == ItemStatus.accepted
                        || i.getStatus() == ItemStatus.rejected);
        if (!allDecided) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "所有条目必须已有验收结论");
        }

        batch.setAcceptedBy(AuthContext.getCurrentUserId());
        batch.setAcceptedAt(OffsetDateTime.now());
        recalculateBatchStatus(batch, items);
        batchMapper.updateById(batch);

        return toBatchResponse(batch, true);
    }

    /**
     * 导出接收回执（骨架，暂返回空数据）。
     */
    public byte[] exportReceipt(Long batchId) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单不存在");
        }
        if (batch.getStatus() != BatchStatus.received
                && batch.getStatus() != BatchStatus.partially_received) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有已接收或部分接收的清单可以导出回执");
        }
        // PDF 生成待后续实现
        return new byte[0];
    }

    // ==================== 征集管理（后台） ====================

    /**
     * 查询征集待办（后台）。
     */
    public PageResult<IntakeBatchResponse> listAdminCollections(BatchPageQuery query) {
        Page<IntakeBatch> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<IntakeBatch> qw = new QueryWrapper<>();
        qw.eq("source_type", SourceType.collection.name());
        qw.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            qw.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(w -> w.like("batch_no", query.getKeyword())
                    .or().like("title", query.getKeyword())
                    .or().like("contact_phone", query.getKeyword()));
        }
        qw.orderByAsc("submitted_at");

        Page<IntakeBatch> result = batchMapper.selectPage(page, qw);
        List<IntakeBatchResponse> voList = result.getRecords().stream()
                .map(b -> toBatchResponse(b, false))
                .collect(Collectors.toList());
        return new PageResult<>(voList, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    /**
     * 约定到馆时间。
     */
    @Transactional
    public IntakeBatchResponse scheduleReceive(Long batchId, CollectionScheduleRequest req) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getSourceType() != SourceType.collection) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "征集清单不存在");
        }
        if (batch.getStatus() != BatchStatus.pending_contact) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有待联系状态的征集清单可以约定到馆时间");
        }

        batch.setStatus(BatchStatus.pending_receive);
        batch.setScheduledReceiveAt(req.getScheduledReceiveAt());
        batchMapper.updateById(batch);

        return toBatchResponse(batch, true);
    }

    /**
     * 拒绝征集意向。
     */
    @Transactional
    public IntakeBatchResponse rejectCollection(Long batchId, CollectionRejectRequest req) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getSourceType() != SourceType.collection) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "征集清单不存在");
        }
        if (batch.getStatus() != BatchStatus.pending_contact) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有待联系状态的征集清单可以拒绝");
        }

        batch.setStatus(BatchStatus.rejected);
        batch.setRejectReason(req.getRejectReason());
        batchMapper.updateById(batch);

        return toBatchResponse(batch, true);
    }

    // ==================== 批次状态重算 ====================

    /**
     * 根据条目状态汇总重算批次状态。
     */
    private void recalculateBatchStatus(IntakeBatch batch, List<IntakeItem> items) {
        long accepted = items.stream().filter(i -> i.getStatus() == ItemStatus.accepted).count();
        long rejected = items.stream().filter(i -> i.getStatus() == ItemStatus.rejected).count();
        long total = items.size();

        if (accepted == total) {
            batch.setStatus(BatchStatus.received);
        } else if (rejected == total) {
            batch.setStatus(BatchStatus.rejected);
        } else if (accepted > 0 && rejected > 0) {
            batch.setStatus(BatchStatus.partially_received);
        } else if (accepted > 0) {
            batch.setStatus(BatchStatus.received);
        } else {
            batch.setStatus(BatchStatus.rejected);
        }
    }

    // ==================== 转换工具 ====================

    private IntakeBatchResponse toBatchResponse(IntakeBatch batch, boolean withItems) {
        IntakeBatchResponse resp = new IntakeBatchResponse();
        resp.setId(batch.getId());
        resp.setBatchNo(batch.getBatchNo());
        resp.setSourceType(batch.getSourceType() != null ? batch.getSourceType().name() : null);
        resp.setTitle(batch.getTitle());
        resp.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
        resp.setStatusText(resolveStatusText(batch.getSourceType(), batch.getStatus()));
        resp.setOrganizationId(batch.getOrganizationId());
        resp.setDepartmentName(batch.getDepartmentName());
        resp.setPublicUserId(batch.getPublicUserId());
        resp.setContactName(batch.getContactName());
        resp.setContactPhone(batch.getContactPhone());
        resp.setArchiveYear(batch.getArchiveYear());
        resp.setExpectedTransferDate(batch.getExpectedTransferDate());
        resp.setScheduledReceiveAt(batch.getScheduledReceiveAt());
        resp.setSubmittedAt(batch.getSubmittedAt());
        resp.setAcceptedBy(batch.getAcceptedBy());
        resp.setAcceptedAt(batch.getAcceptedAt());
        resp.setArchivedAt(batch.getArchivedAt());
        resp.setShelvedAt(batch.getShelvedAt());
        resp.setRejectReason(batch.getRejectReason());
        resp.setAgreementAcceptedAt(batch.getAgreementAcceptedAt());
        resp.setCreatedAt(batch.getCreatedAt());
        resp.setUpdatedAt(batch.getUpdatedAt());

        if (withItems) {
            List<IntakeItem> items = itemMapper.selectList(
                    new QueryWrapper<IntakeItem>().eq("batch_id", batch.getId()).orderByAsc("item_no"));
            resp.setItems(items.stream().map(this::toItemResponse).collect(Collectors.toList()));
            resp.setItemCount(items.size());
        } else {
            Long count = itemMapper.selectCount(
                    new QueryWrapper<IntakeItem>().eq("batch_id", batch.getId()));
            resp.setItemCount(count != null ? count.intValue() : 0);
        }

        return resp;
    }

    private IntakeItemResponse toItemResponse(IntakeItem item) {
        IntakeItemResponse resp = new IntakeItemResponse();
        resp.setId(item.getId());
        resp.setBatchId(item.getBatchId());
        resp.setItemNo(item.getItemNo());
        resp.setStatus(item.getStatus() != null ? item.getStatus().name() : null);
        resp.setStatusText(resolveItemStatusText(item.getStatus()));
        resp.setInputTitle(item.getInputTitle());
        resp.setPageCount(item.getPageCount());
        resp.setRetentionPeriod(item.getRetentionPeriod() != null ? item.getRetentionPeriod().name() : null);
        resp.setCarrierStatus(item.getCarrierStatus() != null ? item.getCarrierStatus().name() : null);
        resp.setSecurityLevel(item.getSecurityLevel());
        resp.setOpenStatus(item.getOpenStatus());
        resp.setAllowDigitization(item.getAllowDigitization());
        resp.setElectronicFormat(item.getElectronicFormat());
        resp.setExpectedFilename(item.getExpectedFilename());
        resp.setFormedDate(item.getFormedDate());
        resp.setAcceptanceNote(item.getAcceptanceNote());
        resp.setRejectReason(item.getRejectReason());
        resp.setFileMatchStatus(item.getFileMatchStatus() != null ? item.getFileMatchStatus().name() : null);
        resp.setAiSuggestion(item.getAiSuggestion());
        resp.setConfirmedTitle(item.getConfirmedTitle());
        resp.setConfirmedResponsibleText(item.getConfirmedResponsibleText());
        resp.setConfirmedFormedDate(item.getConfirmedFormedDate());
        resp.setConfirmedCategoryId(item.getConfirmedCategoryId());
        resp.setConfirmedTags(item.getConfirmedTags());
        resp.setGeneratedArchiveId(item.getGeneratedArchiveId());
        resp.setCreatedAt(item.getCreatedAt());
        resp.setUpdatedAt(item.getUpdatedAt());
        return resp;
    }

    /**
     * 根据来源类型映射批次状态中文展示。
     */
    private String resolveStatusText(SourceType sourceType, BatchStatus status) {
        if (status == null) {
            return null;
        }
        boolean isTransfer = sourceType == SourceType.transfer;
        return switch (status) {
            case draft -> "草稿";
            case pending_transfer -> isTransfer ? "待移交" : "待移交";
            case pending_contact -> "待联系";
            case pending_receive -> "待接收";
            case received -> "已接收";
            case partially_received -> "部分接收";
            case rejected -> isTransfer ? "已回退" : "已拒绝";
            case archived -> "已入库";
            case shelved -> "已上架";
        };
    }

    /**
     * 条目状态中文映射。
     */
    private String resolveItemStatusText(ItemStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case draft -> "草稿";
            case pending_acceptance -> "待验收";
            case accepted -> "已接收";
            case rejected -> "已回退";
            case pending_archive -> "待入库";
            case archived -> "已入库";
        };
    }

    // ==================== 私有工具 ====================

    private IntakeBatch getBatchAndCheckOwner(Long batchId, SourceType expectedType) {
        IntakeBatch batch = batchMapper.selectById(batchId);
        if (batch == null || batch.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单不存在");
        }
        if (batch.getSourceType() != expectedType) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    expectedType == SourceType.transfer ? "移交清单不存在" : "征集清单不存在");
        }

        // 移交清单校验所属单位
        if (expectedType == SourceType.transfer) {
            Long orgId = AuthContext.getOrganizationId();
            if (!batch.getOrganizationId().equals(orgId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作本单位的移交清单");
            }
        }

        return batch;
    }

    private void checkDraftStatus(IntakeBatch batch) {
        if (batch.getStatus() != BatchStatus.draft) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有草稿状态的清单可以编辑");
        }
    }

    private IntakeItem getItemAndCheckBatch(Long itemId, Long batchId) {
        IntakeItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getBatchId().equals(batchId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "条目不存在");
        }
        return item;
    }

    private void insertTransferItem(Long batchId, int itemNo, TransferItemRequest req) {
        IntakeItem item = buildTransferItem(batchId, itemNo, req);
        itemMapper.insert(item);
    }

    private IntakeItem buildTransferItem(Long batchId, int itemNo, TransferItemRequest req) {
        IntakeItem item = new IntakeItem();
        item.setBatchId(batchId);
        item.setItemNo(itemNo);
        item.setStatus(ItemStatus.draft);
        fillTransferItem(item, req);
        return item;
    }

    private void fillTransferItem(IntakeItem item, TransferItemRequest req) {
        item.setInputTitle(req.getInputTitle());
        item.setPageCount(req.getPageCount());
        item.setRetentionPeriod(RetentionPeriod.valueOf(req.getRetentionPeriod()));
        item.setCarrierStatus(CarrierStatus.valueOf(req.getCarrierStatus()));
        item.setSecurityLevel(req.getSecurityLevel());
        item.setOpenStatus(req.getOpenStatus());
        item.setAllowDigitization(req.getAllowDigitization() != null ? req.getAllowDigitization() : false);
        item.setElectronicFormat(req.getElectronicFormat());
        item.setExpectedFilename(req.getExpectedFilename());
        item.setFormedDate(req.getFormedDate());
        item.setFileMatchStatus(FileMatchStatus.none);
    }

    private String getUserName(long userId) {
        // 从 session 或其他方式获取用户名，这里用 ID 简化
        return String.valueOf(userId);
    }
}
