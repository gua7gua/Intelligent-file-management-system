package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ArchiveRequest;
import com.archive.dto.request.BatchShelveRequest;
import com.archive.dto.request.ItemConfirmationRequest;
import com.archive.dto.response.*;
import com.archive.entity.*;
import com.archive.enums.*;
import com.archive.exception.BusinessException;
import com.archive.mapper.*;
import com.archive.util.ArchiveNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 待入库与上架服务。
 * 处理待入库批次列表、确认字段、确认入库（核心事务）、上架确认。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingArchiveService {

    private final IntakeItemMapper intakeItemMapper;
    private final IntakeBatchMapper intakeBatchMapper;
    private final ArchiveMapper archiveMapper;
    private final ArchiveFileService archiveFileService;
    private final ArchiveNoUtil archiveNoUtil;
    private final ArchiveBoxMapper archiveBoxMapper;
    private final ArchiveBoxItemMapper archiveBoxItemMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final StagingFileMapper stagingFileMapper;
    private final TagMapper tagMapper;
    private final ArchiveTagMapper archiveTagMapper;
    private final OrganizationMapper organizationMapper;
    private final CategoryMapper categoryMapper;
    private final FondsMapper fondsMapper;
    private final AuditService auditService;
    private final AiTaskMapper aiTaskMapper;

    // ==================== 9.1 查询待入库批次 ====================

    /**
     * 查询待入库批次列表。
     * 默认按 received/partially_received 过滤（向后兼容）；
     * status="archived" 时改为查 archived 批次，并回填 pendingShelfCount（含 pending_shelf 档案数）。
     */
    public PageResult<PendingBatchResponse> listPendingBatches(
            String sourceType, String aiStatus, String keyword, String status,
            int pageNo, int pageSize) {

        boolean archivedMode = status != null && !status.isBlank()
                && BatchStatus.archived.name().equalsIgnoreCase(status);

        // 查询批次
        QueryWrapper<IntakeBatch> bw = new QueryWrapper<>();
        if (archivedMode) {
            bw.eq("status", BatchStatus.archived.name());
        } else {
            bw.in("status", BatchStatus.received.name(), BatchStatus.partially_received.name());
        }
        if (sourceType != null && !sourceType.isBlank()) {
            bw.eq("source_type", sourceType);
        }
        if (keyword != null && !keyword.isBlank()) {
            bw.and(w -> w.like("batch_no", keyword).or().like("title", keyword));
        }
        bw.orderByDesc("accepted_at");

        Page<IntakeBatch> page = intakeBatchMapper.selectPage(
                new Page<>(pageNo, pageSize), bw);

        List<PendingBatchResponse> records = new ArrayList<>();
        for (IntakeBatch batch : page.getRecords()) {
            PendingBatchResponse resp = new PendingBatchResponse();
            resp.setId(batch.getId());
            resp.setBatchNo(batch.getBatchNo());
            resp.setSourceType(batch.getSourceType() != null ? batch.getSourceType().name() : null);
            resp.setTitle(batch.getTitle());
            resp.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
            resp.setAcceptedAt(batch.getAcceptedAt());

            // 组织名称
            if (batch.getOrganizationId() != null) {
                Organization org = organizationMapper.selectById(batch.getOrganizationId());
                resp.setOrganizationName(org != null ? org.getOrgName() : null);
            }
            resp.setContactName(batch.getContactName());

            // 统计条目
            QueryWrapper<IntakeItem> iw = new QueryWrapper<>();
            iw.eq("batch_id", batch.getId());
            List<IntakeItem> items = intakeItemMapper.selectList(iw);

            long itemCount = items.stream()
                    .filter(i -> i.getStatus() == ItemStatus.accepted
                            || i.getStatus() == ItemStatus.pending_archive
                            || i.getStatus() == ItemStatus.archived)
                    .count();
            long archivedCount = items.stream()
                    .filter(i -> i.getStatus() == ItemStatus.archived)
                    .count();
            long pendingCount = items.stream()
                    .filter(i -> i.getStatus() == ItemStatus.accepted
                            || i.getStatus() == ItemStatus.pending_archive)
                    .count();

            resp.setItemCount((int) itemCount);
            resp.setArchivedCount((int) archivedCount);
            resp.setPendingArchiveCount((int) pendingCount);

            // archived 模式下回填 pendingShelfCount：该批次已生成档案中 lifecycle_status=pending_shelf 的数量
            if (archivedMode) {
                resp.setPendingShelfCount(countPendingShelf(items));
            } else {
                resp.setPendingShelfCount(0);
            }

            // 关联查询该批次最近一次 AI 补全任务状态（business_type=intake_batch）
            AiTask latestAiTask = aiTaskMapper.selectOne(new QueryWrapper<AiTask>()
                    .eq("business_type", "intake_batch")
                    .eq("business_id", batch.getId())
                    .orderByDesc("started_at")
                    .last("limit 1"));
            resp.setLatestAiTaskStatus(latestAiTask != null ? latestAiTask.getStatus() : null);

            records.add(resp);
        }

        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    /**
     * 统计条目对应档案中 lifecycle_status=pending_shelf 的数量。
     * 仅对已入库条目（status=archived 且 generatedArchiveId 非空）查询。
     */
    private int countPendingShelf(List<IntakeItem> items) {
        List<Long> archiveIds = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.archived)
                .map(IntakeItem::getGeneratedArchiveId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (archiveIds.isEmpty()) {
            return 0;
        }
        QueryWrapper<Archive> aw = new QueryWrapper<>();
        aw.in("id", archiveIds);
        aw.eq("lifecycle_status", LifecycleStatus.pending_shelf.name());
        return Math.toIntExact(archiveMapper.selectCount(aw));
    }

    // ==================== 9.2 获取入库批次详情 ====================

    /**
     * 获取入库批次详情，含条目、暂存文件、可用盒号/架位。
     */
    public PendingBatchDetailResponse getBatchDetail(Long batchId) {
        IntakeBatch batch = intakeBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单批次不存在");
        }

        PendingBatchDetailResponse resp = new PendingBatchDetailResponse();
        resp.setId(batch.getId());
        resp.setBatchNo(batch.getBatchNo());
        resp.setSourceType(batch.getSourceType() != null ? batch.getSourceType().name() : null);
        resp.setTitle(batch.getTitle());
        resp.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
        resp.setAcceptedAt(batch.getAcceptedAt());
        resp.setContactName(batch.getContactName());

        if (batch.getOrganizationId() != null) {
            Organization org = organizationMapper.selectById(batch.getOrganizationId());
            resp.setOrganizationName(org != null ? org.getOrgName() : null);
        }

        // 已接收/待入库/已入库条目
        QueryWrapper<IntakeItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batchId);
        iw.in("status", ItemStatus.accepted.name(), ItemStatus.pending_archive.name(),
                ItemStatus.archived.name());
        iw.orderByAsc("item_no");
        List<IntakeItem> items = intakeItemMapper.selectList(iw);

        List<PendingItemResponse> itemResponses = new ArrayList<>();
        for (IntakeItem item : items) {
            PendingItemResponse ir = toItemResponse(item);
            itemResponses.add(ir);
        }
        resp.setItems(itemResponses);

        // 可用档案盒（未满的）
        QueryWrapper<ArchiveBox> boxW = new QueryWrapper<>();
        boxW.eq("status", "normal");
        boxW.orderByAsc("box_no");
        List<ArchiveBox> boxes = archiveBoxMapper.selectList(boxW);
        List<PendingBatchDetailResponse.AvailableBoxSummary> boxSummaries = boxes.stream()
                .map(b -> {
                    PendingBatchDetailResponse.AvailableBoxSummary s = new PendingBatchDetailResponse.AvailableBoxSummary();
                    s.setId(b.getId());
                    s.setBoxNo(b.getBoxNo());
                    s.setCategoryId(b.getCategoryId());
                    s.setFondsId(b.getFondsId());
                    s.setUsedCount(b.getUsedCount());
                    s.setCapacity(b.getCapacity());
                    return s;
                }).collect(Collectors.toList());
        resp.setAvailableBoxes(boxSummaries);

        // 可用架位（未被档案盒占用的 active 架位）
        QueryWrapper<StorageLocation> locW = new QueryWrapper<>();
        locW.eq("status", "active");
        locW.orderByAsc("location_code");
        List<StorageLocation> locations = storageLocationMapper.selectList(locW);
        // 过滤掉已被占用的架位
        Set<Long> occupiedLocationIds = boxes.stream()
                .filter(b -> b.getLocationId() != null)
                .map(ArchiveBox::getLocationId)
                .collect(Collectors.toSet());
        List<PendingBatchDetailResponse.AvailableLocationSummary> locSummaries = locations.stream()
                .filter(l -> !occupiedLocationIds.contains(l.getId()))
                .map(l -> {
                    PendingBatchDetailResponse.AvailableLocationSummary s = new PendingBatchDetailResponse.AvailableLocationSummary();
                    s.setId(l.getId());
                    s.setLocationCode(l.getLocationCode());
                    s.setRoomId(l.getRoomId());
                    return s;
                }).collect(Collectors.toList());
        resp.setAvailableLocations(locSummaries);

        return resp;
    }

    // ==================== 9.6 确认条目入库字段 ====================

    /**
     * 确认条目入库字段。
     * 状态变化：accepted → pending_archive。
     */
    @Transactional
    public PendingItemResponse confirmItemFields(Long itemId, ItemConfirmationRequest req) {
        IntakeItem item = intakeItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单条目不存在");
        }
        // 允许 accepted 或 pending_archive 重新确认入库字段：
        // 否则用户确认后发现所选档案盒分类不一致（confirmArchive 抛「同盒档案分类必须一致」），
        // 既无法改 categoryId（已非 accepted）也无法改盒匹配，流程卡死。见 B13-5。
        if (item.getStatus() != ItemStatus.accepted
                && item.getStatus() != ItemStatus.pending_archive) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "条目状态不是已接收或待入库，无法确认入库字段");
        }

        item.setConfirmedTitle(req.getConfirmedTitle());
        item.setConfirmedResponsibleText(req.getConfirmedResponsibleText());
        item.setConfirmedFormedDate(req.getConfirmedFormedDate());
        item.setConfirmedCategoryId(req.getConfirmedCategoryId());
        item.setConfirmedTags(req.getConfirmedTags());
        item.setStatus(ItemStatus.pending_archive);
        intakeItemMapper.updateById(item);

        auditService.log("M05", "confirm_fields", "intake_item", itemId,
                Map.of("batchId", item.getBatchId()));

        return toItemResponse(item);
    }

    // ==================== 9.7 确认入库（核心事务） ====================

    /**
     * 确认入库。
     * 在一个事务内完成：生成档案、复制文件、装盒预占、回写条目、重算批次。
     */
    @Transactional
    public ArchiveResultResponse confirmArchive(Long itemId, ArchiveRequest req) {
        // 1. 校验条目
        IntakeItem item = intakeItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单条目不存在");
        }
        if (item.getStatus() != ItemStatus.pending_archive) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "条目状态不是待入库，请先确认入库字段");
        }
        if (item.getGeneratedArchiveId() != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该条目已入库，禁止重复入库");
        }

        // 2. 读取批次
        IntakeBatch batch = intakeBatchMapper.selectById(item.getBatchId());
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "来源批次不存在");
        }

        // 3. 读取已匹配暂存文件
        QueryWrapper<StagingFile> sfw = new QueryWrapper<>();
        sfw.eq("batch_id", batch.getId());
        sfw.eq("item_id", itemId);
        sfw.eq("match_status", MatchStatus.matched.name());
        List<StagingFile> stagingFiles = stagingFileMapper.selectList(sfw);

        // 4. 生成档号
        String archiveNo = archiveNoUtil.generate();

        // 5. 构建 Archive 实体
        Archive archive = new Archive();
        archive.setArchiveNo(archiveNo);
        // 字段来源优先级：confirmed > input > 条目
        archive.setTitle(firstNonNull(item.getConfirmedTitle(), item.getInputTitle()));
        archive.setResponsibleText(item.getConfirmedResponsibleText());
        archive.setFormedDate(firstNonNull(item.getConfirmedFormedDate(), item.getFormedDate()));
        archive.setFormedYear(archive.getFormedDate() != null
                ? archive.getFormedDate().getYear() : null);
        archive.setCategoryId(item.getConfirmedCategoryId());
        archive.setSourceType(batch.getSourceType());
        archive.setSourceBatchId(batch.getId());
        archive.setSourceItemId(itemId);
        archive.setOrganizationId(batch.getOrganizationId());
        archive.setFondsId(req.getFondsId());
        archive.setCarrierStatus(item.getCarrierStatus());
        archive.setRetentionPeriod(item.getRetentionPeriod());
        archive.setSecurityLevel(item.getSecurityLevel() != null ? item.getSecurityLevel() : 0);
        archive.setOpenStatus(item.getOpenStatus() != null ? item.getOpenStatus() : "open");
        archive.setAllowDigitization(item.getAllowDigitization() != null ? item.getAllowDigitization() : false);
        archive.setLoanStatus(LoanStatus.available);
        archive.setConditionStatus(ConditionStatus.normal);
        archive.setArchivedAt(OffsetDateTime.now());

        // 生命周期：纯电子 → normal，纸质相关 → pending_shelf
        boolean isElectronic = item.getCarrierStatus() == CarrierStatus.electronic;
        if (isElectronic) {
            archive.setLifecycleStatus(LifecycleStatus.normal);
        } else {
            archive.setLifecycleStatus(LifecycleStatus.pending_shelf);
        }

        // 计算 retention_until
        archive.setRetentionUntil(calculateRetentionUntil(
                item.getRetentionPeriod(), archive.getArchivedAt()));

        archiveMapper.insert(archive);

        // 6. 复制暂存文件为正式文件
        if (!stagingFiles.isEmpty()) {
            archiveFileService.stagingToFormal(archive, stagingFiles);
        }

        // 7. 纸质相关档案：装盒预占
        if (!isElectronic) {
            if (req.getBoxId() == null || req.getLocationId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "纸质相关档案必须提供盒号和架位");
            }

            ArchiveBox box = archiveBoxMapper.selectById(req.getBoxId());
            if (box == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "档案盒不存在");
            }

            // 校验同盒分类/全宗一致
            if (box.getCategoryId() != null && item.getConfirmedCategoryId() != null
                    && !box.getCategoryId().equals(item.getConfirmedCategoryId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "同盒档案分类必须一致");
            }
            if (box.getFondsId() != null && req.getFondsId() != null
                    && !box.getFondsId().equals(req.getFondsId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "同盒档案全宗必须一致");
            }

            // 创建盒内关系
            ArchiveBoxItem boxItem = new ArchiveBoxItem();
            boxItem.setBoxId(req.getBoxId());
            boxItem.setArchiveId(archive.getId());
            // 默认 sortNo：未显式传入时，按盒内已有最大 sort_no + 1 自动递增，
            // 避免同盒第二件档案因默认值 1 与已有项冲突（违反 uk_archive_box_items_box_sort）。
            int sortNo = req.getSortNo() != null ? req.getSortNo() : nextBoxSortNo(req.getBoxId());
            boxItem.setSortNo(sortNo);
            boxItem.setPageCount(req.getPageCount() != null ? req.getPageCount() : item.getPageCount());
            boxItem.setPhysicalStatus("normal");
            archiveBoxItemMapper.insert(boxItem);

            // 更新档案盒 used_count
            int newCount = (box.getUsedCount() != null ? box.getUsedCount() : 0) + 1;
            box.setUsedCount(newCount);
            if (box.getCapacity() != null && newCount >= box.getCapacity()) {
                box.setStatus("full");
            }
            archiveBoxMapper.updateById(box);
        }

        // 8. 处理标签
        if (item.getConfirmedTags() != null && !item.getConfirmedTags().isEmpty()) {
            for (String tagName : item.getConfirmedTags()) {
                // 查找或创建标签
                QueryWrapper<Tag> tw = new QueryWrapper<>();
                tw.eq("tag_name", tagName);
                Tag tag = tagMapper.selectOne(tw);
                if (tag == null) {
                    tag = new Tag();
                    tag.setTagName(tagName);
                    tag.setCreatedAt(OffsetDateTime.now());
                    tagMapper.insert(tag);
                }
                // 写入 archive_tags
                ArchiveTag at = new ArchiveTag();
                at.setArchiveId(archive.getId());
                at.setTagId(tag.getId());
                at.setCreatedAt(OffsetDateTime.now());
                archiveTagMapper.insert(at);
            }
        }

        // 9. 回写清单条目
        item.setGeneratedArchiveId(archive.getId());
        item.setStatus(ItemStatus.archived);
        intakeItemMapper.updateById(item);

        // 10. 重算批次状态
        recalculateBatchStatus(batch);

        // 11. 审计日志
        auditService.log("M05", "confirm_archive", "archive", archive.getId(),
                Map.of("archiveNo", archiveNo, "itemId", itemId, "batchId", batch.getId()));

        ArchiveResultResponse result = new ArchiveResultResponse();
        result.setArchiveId(archive.getId());
        result.setArchiveNo(archiveNo);
        result.setLifecycleStatus(archive.getLifecycleStatus().name());
        return result;
    }

    // ==================== 9.8 批次确认上架 ====================

    /**
     * 批次确认上架。
     * 纸质相关档案 pending_shelf → normal，批次 archived → shelved。
     */
    @Transactional
    public void shelveBatch(Long batchId, BatchShelveRequest req) {
        IntakeBatch batch = intakeBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "清单批次不存在");
        }
        if (batch.getStatus() != BatchStatus.archived) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "批次状态不是已入库，无法上架");
        }

        // 查询该批次关联的所有 pending_shelf 档案
        QueryWrapper<IntakeItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batchId);
        iw.eq("status", ItemStatus.archived.name());
        iw.isNotNull("generated_archive_id");
        List<IntakeItem> archivedItems = intakeItemMapper.selectList(iw);

        if (archivedItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该批次没有已入库的条目");
        }

        List<Long> archiveIds = archivedItems.stream()
                .map(IntakeItem::getGeneratedArchiveId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 更新所有 pending_shelf 档案为 normal
        OffsetDateTime now = OffsetDateTime.now();
        for (Long archiveId : archiveIds) {
            Archive archive = archiveMapper.selectById(archiveId);
            if (archive != null && archive.getLifecycleStatus() == LifecycleStatus.pending_shelf) {
                archive.setLifecycleStatus(LifecycleStatus.normal);
                archive.setShelvedAt(now);
                archiveMapper.updateById(archive);
            }
        }

        // 批次状态 → shelved
        batch.setStatus(BatchStatus.shelved);
        batch.setShelvedAt(now);
        intakeBatchMapper.updateById(batch);

        auditService.log("M05", "shelve_batch", "intake_batch", batchId,
                Map.of("note", req.getNote() != null ? req.getNote() : ""));
    }

    // ==================== 内部方法 ====================

    /**
     * 重算批次状态（考虑 archived 状态）。
     * 规则：
     * - 全部 archived → archived
     * - 全部 rejected → rejected
     * - 存在 accepted/pending_archive 且存在 rejected → partially_received
     * - 存在 accepted/pending_archive 且无 rejected → received
     * - 存在 archived 且无 accepted/pending_archive → archived（全部可处理的已完成）
     */
    private int nextBoxSortNo(Long boxId) {
        QueryWrapper<ArchiveBoxItem> w = new QueryWrapper<>();
        w.eq("box_id", boxId).orderByDesc("sort_no").last("LIMIT 1");
        ArchiveBoxItem latest = archiveBoxItemMapper.selectOne(w);
        return latest != null && latest.getSortNo() != null ? latest.getSortNo() + 1 : 1;
    }

    private void recalculateBatchStatus(IntakeBatch batch) {
        QueryWrapper<IntakeItem> iw = new QueryWrapper<>();
        iw.eq("batch_id", batch.getId());
        List<IntakeItem> items = intakeItemMapper.selectList(iw);

        long archived = items.stream().filter(i -> i.getStatus() == ItemStatus.archived).count();
        long rejected = items.stream().filter(i -> i.getStatus() == ItemStatus.rejected).count();
        long pending = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.accepted
                        || i.getStatus() == ItemStatus.pending_archive)
                .count();

        if (pending == 0 && rejected == 0 && archived > 0) {
            // 全部已入库
            batch.setStatus(BatchStatus.archived);
            batch.setArchivedAt(OffsetDateTime.now());
        } else if (pending == 0 && rejected > 0 && archived > 0) {
            // 部分入库、部分回退，无待处理 → 归为已入库
            batch.setStatus(BatchStatus.archived);
            batch.setArchivedAt(OffsetDateTime.now());
        } else if (pending == 0 && archived == 0 && rejected > 0) {
            batch.setStatus(BatchStatus.rejected);
        } else if (pending > 0 && rejected > 0) {
            batch.setStatus(BatchStatus.partially_received);
        } else if (pending > 0) {
            batch.setStatus(BatchStatus.received);
        }

        intakeBatchMapper.updateById(batch);
    }

    /**
     * 计算到期日。
     */
    private LocalDate calculateRetentionUntil(RetentionPeriod period, OffsetDateTime archivedAt) {
        if (period == null || period == RetentionPeriod.permanent) {
            return null;
        }
        LocalDate base = archivedAt.toLocalDate();
        return switch (period) {
            case _10y -> base.plusYears(10);
            case _30y -> base.plusYears(30);
            default -> null;
        };
    }

    /**
     * 转换条目为响应 DTO。
     */
    private PendingItemResponse toItemResponse(IntakeItem item) {
        PendingItemResponse r = new PendingItemResponse();
        r.setId(item.getId());
        r.setItemNo(item.getItemNo());
        r.setInputTitle(item.getInputTitle());
        r.setStatus(item.getStatus() != null ? item.getStatus().name() : null);
        r.setCarrierStatus(item.getCarrierStatus() != null ? item.getCarrierStatus().name() : null);
        r.setPageCount(item.getPageCount());
        r.setSecurityLevel(item.getSecurityLevel());
        r.setRetentionPeriod(item.getRetentionPeriod() != null ? item.getRetentionPeriod().getDbValue() : null);
        r.setOpenStatus(item.getOpenStatus() != null ? item.getOpenStatus() : "open");
        r.setAllowDigitization(item.getAllowDigitization() != null ? item.getAllowDigitization() : false);
        r.setFileMatchStatus(item.getFileMatchStatus() != null ? item.getFileMatchStatus().name() : null);
        r.setAiSuggestion(item.getAiSuggestion() != null ? item.getAiSuggestion().toString() : null);
        // 拆出结构化 AI 建议字段，供前端表单回填
        Map<String, Object> ai = item.getAiSuggestion();
        if (ai != null) {
            Object title = ai.get("title");
            if (title != null) r.setSuggestedTitle(String.valueOf(title));
            Object responsible = ai.get("responsible");
            if (responsible != null) r.setSuggestedResponsible(String.valueOf(responsible));
            Object formedDate = ai.get("formedDate");
            if (formedDate instanceof String s && !s.isBlank()) {
                try {
                    r.setSuggestedFormedDate(LocalDate.parse(s));
                } catch (Exception ignore) {
                    // 日期格式不规范时忽略，不阻断响应
                }
            }
            Object categoryId = ai.get("categoryId");
            if (categoryId instanceof Number n) r.setSuggestedCategoryId(n.intValue());
            Object tags = ai.get("tags");
            if (tags instanceof List<?> list) {
                List<String> tagList = new ArrayList<>();
                for (Object t : list) {
                    if (t != null) tagList.add(String.valueOf(t));
                }
                if (!tagList.isEmpty()) r.setSuggestedTags(tagList);
            }
        }
        r.setConfirmedTitle(item.getConfirmedTitle());
        r.setConfirmedResponsibleText(item.getConfirmedResponsibleText());
        r.setConfirmedFormedDate(item.getConfirmedFormedDate());
        r.setConfirmedCategoryId(item.getConfirmedCategoryId());
        r.setConfirmedTags(item.getConfirmedTags());
        r.setGeneratedArchiveId(item.getGeneratedArchiveId());

        // 已入库档案的生命周期状态
        if (item.getGeneratedArchiveId() != null) {
            Archive ar = archiveMapper.selectById(item.getGeneratedArchiveId());
            if (ar != null && ar.getLifecycleStatus() != null) {
                r.setLifecycleStatus(ar.getLifecycleStatus().name());
            }
        }

        // 已匹配暂存文件摘要
        QueryWrapper<StagingFile> sfw = new QueryWrapper<>();
        sfw.eq("item_id", item.getId());
        sfw.eq("match_status", MatchStatus.matched.name());
        List<StagingFile> files = stagingFileMapper.selectList(sfw);
        List<PendingItemResponse.StagingFileSummary> fileSummaries = files.stream()
                .map(sf -> {
                    PendingItemResponse.StagingFileSummary s = new PendingItemResponse.StagingFileSummary();
                    s.setId(sf.getId());
                    s.setOriginalFilename(sf.getOriginalFilename());
                    s.setFileSize(sf.getFileSize());
                    s.setFileExt(sf.getFileExt());
                    s.setMatchStatus(sf.getMatchStatus() != null ? sf.getMatchStatus().name() : null);
                    return s;
                }).collect(Collectors.toList());
        r.setStagingFiles(fileSummaries);

        return r;
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static LocalDate firstNonNull(LocalDate... values) {
        for (LocalDate v : values) {
            if (v != null) return v;
        }
        return null;
    }
}
