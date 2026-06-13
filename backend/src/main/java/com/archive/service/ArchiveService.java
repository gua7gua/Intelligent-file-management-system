package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ArchiveUpdateRequest;
import com.archive.dto.request.OpenAdjustRequest;
import com.archive.dto.request.SecurityAdjustRequest;
import com.archive.dto.response.ArchiveResponse;
import com.archive.dto.response.ArchiveFileResponse;
import com.archive.entity.*;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.LifecycleStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 档案管理服务。
 * 处理档案列表、详情、元数据编辑、密级/开放调整审批发起。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveMapper archiveMapper;
    private final ArchiveFileMapper archiveFileMapper;
    private final ArchiveTagMapper archiveTagMapper;
    private final TagMapper tagMapper;
    private final ArchiveChangeLogMapper archiveChangeLogMapper;
    private final ApprovalRequestMapper approvalRequestMapper;
    private final IntakeItemMapper intakeItemMapper;
    private final IntakeBatchMapper intakeBatchMapper;
    private final OrganizationMapper organizationMapper;
    private final CategoryMapper categoryMapper;
    private final FondsMapper fondsMapper;
    private final ArchiveBoxItemMapper archiveBoxItemMapper;
    private final ArchiveBoxMapper archiveBoxMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final AuditService auditService;

    // ==================== 10.1 管理端查询档案 ====================

    public PageResult<ArchiveResponse> listArchives(
            String keyword, String archiveNo, Integer categoryId,
            Integer formedYearStart, Integer formedYearEnd,
            Long organizationId, Long fondsId, Integer securityLevel,
            String openStatus, String carrierStatus, String lifecycleStatus,
            String loanStatus, String conditionStatus,
            int pageNo, int pageSize) {

        QueryWrapper<Archive> w = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like("title", keyword)
                    .or().like("archive_no", keyword)
                    .or().like("responsible_text", keyword));
        }
        if (archiveNo != null && !archiveNo.isBlank()) {
            w.like("archive_no", archiveNo);
        }
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (formedYearStart != null) {
            w.ge("formed_year", formedYearStart);
        }
        if (formedYearEnd != null) {
            w.le("formed_year", formedYearEnd);
        }
        if (organizationId != null) {
            w.eq("organization_id", organizationId);
        }
        if (fondsId != null) {
            w.eq("fonds_id", fondsId);
        }
        if (securityLevel != null) {
            w.eq("security_level", securityLevel);
        }
        if (openStatus != null && !openStatus.isBlank()) {
            w.eq("open_status", openStatus);
        }
        if (carrierStatus != null && !carrierStatus.isBlank()) {
            w.eq("carrier_status", carrierStatus);
        }
        if (lifecycleStatus != null && !lifecycleStatus.isBlank()) {
            w.eq("lifecycle_status", lifecycleStatus);
        }
        if (loanStatus != null && !loanStatus.isBlank()) {
            w.eq("loan_status", loanStatus);
        }
        if (conditionStatus != null && !conditionStatus.isBlank()) {
            w.eq("condition_status", conditionStatus);
        }
        w.orderByDesc("archived_at");

        Page<Archive> page = archiveMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<ArchiveResponse> records = page.getRecords().stream()
                .map(this::toArchiveResponse)
                .collect(Collectors.toList());

        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    // ==================== 10.2 获取档案详情 ====================

    public ArchiveResponse getArchiveDetail(Long archiveId) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在");
        }

        ArchiveResponse resp = toArchiveResponse(archive);

        // 关联文件列表
        QueryWrapper<ArchiveFile> fw = new QueryWrapper<>();
        fw.eq("archive_id", archiveId);
        List<ArchiveFile> files = archiveFileMapper.selectList(fw);
        resp.setFiles(files.stream().map(f -> {
            ArchiveFileResponse fr = new ArchiveFileResponse();
            fr.setId(f.getId());
            fr.setArchiveId(f.getArchiveId());
            fr.setFileRole(f.getFileRole() != null ? f.getFileRole().name() : null);
            fr.setOriginalFilename(f.getOriginalFilename());
            fr.setFileExt(f.getFileExt());
            fr.setMimeType(f.getMimeType());
            fr.setFileSize(f.getFileSize());
            fr.setScanResult(f.getScanResult() != null ? f.getScanResult().name() : null);
            fr.setUsabilityResult(f.getUsabilityResult() != null ? f.getUsabilityResult().name() : null);
            fr.setFileStatus(f.getFileStatus() != null ? f.getFileStatus().name() : null);
            return fr;
        }).collect(Collectors.toList()));

        // 变更日志
        QueryWrapper<ArchiveChangeLog> cw = new QueryWrapper<>();
        cw.eq("archive_id", archiveId);
        cw.orderByDesc("changed_at");
        List<ArchiveChangeLog> changeLogs = archiveChangeLogMapper.selectList(cw);
        resp.setChangeLogs(changeLogs.stream().map(cl -> {
            ArchiveResponse.ArchiveChangeLogEntry e = new ArchiveResponse.ArchiveChangeLogEntry();
            e.setId(cl.getId());
            e.setFieldName(cl.getFieldName());
            e.setOldValue(cl.getOldValue());
            e.setNewValue(cl.getNewValue());
            e.setChangeSource(cl.getChangeSource());
            e.setChangedAt(cl.getChangedAt());
            return e;
        }).collect(Collectors.toList()));

        return resp;
    }

    // ==================== 10.3 编辑非受保护元数据 ====================

    @Transactional
    public ArchiveResponse updateArchive(Long archiveId, ArchiveUpdateRequest req) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在");
        }
        if (archive.getLifecycleStatus() == LifecycleStatus.destroyed) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "已销毁档案禁止编辑");
        }

        // 逐字段比较并记录变更
        recordChange(archiveId, "title", archive.getTitle(), req.getTitle(), req.getChangeReason(), "manual_edit");
        recordChange(archiveId, "responsible_text", archive.getResponsibleText(), req.getResponsibleText(), req.getChangeReason(), "manual_edit");
        recordChange(archiveId, "formed_date",
                archive.getFormedDate() != null ? archive.getFormedDate().toString() : null,
                req.getFormedDate() != null ? req.getFormedDate().toString() : null,
                req.getChangeReason(), "manual_edit");
        recordChange(archiveId, "category_id",
                archive.getCategoryId() != null ? archive.getCategoryId().toString() : null,
                req.getCategoryId() != null ? req.getCategoryId().toString() : null,
                req.getChangeReason(), "manual_edit");
        recordChange(archiveId, "fonds_id",
                archive.getFondsId() != null ? archive.getFondsId().toString() : null,
                req.getFondsId() != null ? req.getFondsId().toString() : null,
                req.getChangeReason(), "manual_edit");

        // 更新字段
        if (req.getTitle() != null) {
            archive.setTitle(req.getTitle());
        }
        if (req.getResponsibleText() != null) {
            archive.setResponsibleText(req.getResponsibleText());
        }
        if (req.getFormedDate() != null) {
            archive.setFormedDate(req.getFormedDate());
            archive.setFormedYear(req.getFormedDate().getYear());
        }
        if (req.getCategoryId() != null) {
            archive.setCategoryId(req.getCategoryId());
        }
        if (req.getFondsId() != null) {
            archive.setFondsId(req.getFondsId());
        }
        archiveMapper.updateById(archive);

        // 处理标签：先删后插
        if (req.getTagNames() != null) {
            archiveTagMapper.deleteByArchiveId(archiveId);
            for (String tagName : req.getTagNames()) {
                QueryWrapper<Tag> tw = new QueryWrapper<>();
                tw.eq("tag_name", tagName);
                Tag tag = tagMapper.selectOne(tw);
                if (tag == null) {
                    tag = new Tag();
                    tag.setTagName(tagName);
                    tag.setCreatedAt(OffsetDateTime.now());
                    tagMapper.insert(tag);
                }
                ArchiveTag at = new ArchiveTag();
                at.setArchiveId(archiveId);
                at.setTagId(tag.getId());
                at.setCreatedAt(OffsetDateTime.now());
                archiveTagMapper.insert(at);
            }
        }

        auditService.log("M05", "update_metadata", "archive", archiveId,
                Map.of("changeReason", req.getChangeReason() != null ? req.getChangeReason() : ""));

        return toArchiveResponse(archive);
    }

    // ==================== 10.4 发起密级调整审批 ====================

    @Transactional
    public ApprovalRequest createSecurityAdjustment(Long archiveId, SecurityAdjustRequest req) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在");
        }

        // 校验新密级不等于当前密级
        if (req.getNewSecurityLevel().equals(archive.getSecurityLevel())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "新密级与当前密级相同");
        }

        // 校验凭证档案
        validateEvidenceArchive(req.getEvidenceArchiveNo(), archive);

        // 校验无重复 pending 审批
        checkNoPendingApproval(ApprovalType.security_adjust, archiveId);

        ApprovalRequest ar = new ApprovalRequest();
        ar.setApprovalType(ApprovalType.security_adjust);
        ar.setTargetType("archive");
        ar.setTargetId(archiveId);
        ar.setEvidenceArchiveId(findArchiveByNo(req.getEvidenceArchiveNo()));
        ar.setOldValue(String.valueOf(archive.getSecurityLevel()));
        ar.setNewValue(String.valueOf(req.getNewSecurityLevel()));
        ar.setReason(req.getReason());
        ar.setStatus(ApprovalStatus.pending);
        ar.setSubmittedBy(com.archive.common.AuthContext.getCurrentUserId());
        ar.setSubmittedAt(OffsetDateTime.now());
        approvalRequestMapper.insert(ar);

        auditService.log("M05", "submit_security_adjustment", "archive", archiveId,
                Map.of("newLevel", req.getNewSecurityLevel(), "reason", req.getReason()));

        return ar;
    }

    // ==================== 10.5 发起开放调整审批 ====================

    @Transactional
    public ApprovalRequest createOpenAdjustment(Long archiveId, OpenAdjustRequest req) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在");
        }

        if (req.getNewOpenStatus().equals(archive.getOpenStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "新开放状态与当前状态相同");
        }

        validateEvidenceArchive(req.getEvidenceArchiveNo(), archive);
        checkNoPendingApproval(ApprovalType.open_adjust, archiveId);

        ApprovalRequest ar = new ApprovalRequest();
        ar.setApprovalType(ApprovalType.open_adjust);
        ar.setTargetType("archive");
        ar.setTargetId(archiveId);
        ar.setEvidenceArchiveId(findArchiveByNo(req.getEvidenceArchiveNo()));
        ar.setOldValue(archive.getOpenStatus());
        ar.setNewValue(req.getNewOpenStatus());
        ar.setReason(req.getReason());
        ar.setStatus(ApprovalStatus.pending);
        ar.setSubmittedBy(com.archive.common.AuthContext.getCurrentUserId());
        ar.setSubmittedAt(OffsetDateTime.now());
        approvalRequestMapper.insert(ar);

        auditService.log("M05", "submit_open_adjustment", "archive", archiveId,
                Map.of("newStatus", req.getNewOpenStatus(), "reason", req.getReason()));

        return ar;
    }

    // ==================== 内部方法 ====================

    /**
     * 校验凭证档案：存在、未销毁、与目标档案组织或全宗匹配。
     */
    private void validateEvidenceArchive(String evidenceArchiveNo, Archive targetArchive) {
        QueryWrapper<Archive> ew = new QueryWrapper<>();
        ew.eq("archive_no", evidenceArchiveNo);
        Archive evidence = archiveMapper.selectOne(ew);
        if (evidence == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "凭证档案不存在: " + evidenceArchiveNo);
        }
        if (evidence.getLifecycleStatus() == LifecycleStatus.destroyed) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "凭证档案已销毁");
        }

        // 匹配规则：两份档案 organization_id 均非空且相同，或 fonds_id 均非空且相同
        boolean orgMatch = targetArchive.getOrganizationId() != null
                && evidence.getOrganizationId() != null
                && targetArchive.getOrganizationId().equals(evidence.getOrganizationId());
        boolean fondsMatch = targetArchive.getFondsId() != null
                && evidence.getFondsId() != null
                && targetArchive.getFondsId().equals(evidence.getFondsId());
        if (!orgMatch && !fondsMatch) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "凭证档案与目标档案的组织或全宗不匹配");
        }
    }

    /**
     * 校验同一档案同类审批无 pending 记录。
     */
    private void checkNoPendingApproval(ApprovalType type, Long archiveId) {
        QueryWrapper<ApprovalRequest> aw = new QueryWrapper<>();
        aw.eq("approval_type", type.name());
        aw.eq("target_type", "archive");
        aw.eq("target_id", archiveId);
        aw.eq("status", ApprovalStatus.pending.name());
        Long count = approvalRequestMapper.selectCount(aw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该档案已有待审批的同类型申请");
        }
    }

    /**
     * 根据档号查找档案 ID。
     */
    private Long findArchiveByNo(String archiveNo) {
        QueryWrapper<Archive> w = new QueryWrapper<>();
        w.eq("archive_no", archiveNo);
        Archive a = archiveMapper.selectOne(w);
        return a != null ? a.getId() : null;
    }

    /**
     * 记录字段变更日志（仅当新旧值不同时）。
     */
    private void recordChange(Long archiveId, String fieldName, String oldValue, String newValue,
                              String reason, String source) {
        if (!Objects.equals(oldValue, newValue)) {
            ArchiveChangeLog log = new ArchiveChangeLog();
            log.setArchiveId(archiveId);
            log.setFieldName(fieldName);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setChangeReason(reason);
            log.setChangeSource(source);
            log.setChangedBy(com.archive.common.AuthContext.getCurrentUserId());
            log.setChangedAt(OffsetDateTime.now());
            archiveChangeLogMapper.insert(log);
        }
    }

    /**
     * 转换为 ArchiveResponse（列表级，不含文件和变更日志）。
     */
    private ArchiveResponse toArchiveResponse(Archive archive) {
        ArchiveResponse resp = new ArchiveResponse();
        resp.setId(archive.getId());
        resp.setArchiveNo(archive.getArchiveNo());
        resp.setTitle(archive.getTitle());
        resp.setResponsibleText(archive.getResponsibleText());
        resp.setFormedDate(archive.getFormedDate());
        resp.setFormedYear(archive.getFormedYear());
        resp.setCategoryId(archive.getCategoryId());
        resp.setSourceType(archive.getSourceType() != null ? archive.getSourceType().name() : null);
        resp.setOrganizationId(archive.getOrganizationId());
        resp.setFondsId(archive.getFondsId());
        resp.setCarrierStatus(archive.getCarrierStatus() != null ? archive.getCarrierStatus().name() : null);
        resp.setRetentionPeriod(archive.getRetentionPeriod() != null ? archive.getRetentionPeriod().name() : null);
        resp.setRetentionUntil(archive.getRetentionUntil());
        resp.setSecurityLevel(archive.getSecurityLevel());
        resp.setOpenStatus(archive.getOpenStatus());
        resp.setAllowDigitization(archive.getAllowDigitization());
        resp.setLifecycleStatus(archive.getLifecycleStatus() != null ? archive.getLifecycleStatus().name() : null);
        resp.setLoanStatus(archive.getLoanStatus() != null ? archive.getLoanStatus().name() : null);
        resp.setConditionStatus(archive.getConditionStatus() != null ? archive.getConditionStatus().name() : null);
        resp.setArchivedAt(archive.getArchivedAt());
        resp.setShelvedAt(archive.getShelvedAt());

        // 关联名称
        if (archive.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(archive.getCategoryId());
            resp.setCategoryName(cat != null ? cat.getCategoryName() : null);
        }
        if (archive.getOrganizationId() != null) {
            Organization org = organizationMapper.selectById(archive.getOrganizationId());
            resp.setOrganizationName(org != null ? org.getOrgName() : null);
        }
        if (archive.getFondsId() != null) {
            Fonds fonds = fondsMapper.selectById(archive.getFondsId());
            resp.setFondsName(fonds != null ? fonds.getFondsName() : null);
        }

        // 标签
        QueryWrapper<ArchiveTag> atw = new QueryWrapper<>();
        atw.eq("archive_id", archive.getId());
        List<ArchiveTag> archiveTags = archiveTagMapper.selectList(atw);
        List<String> tagNames = new ArrayList<>();
        for (ArchiveTag at : archiveTags) {
            Tag tag = tagMapper.selectById(at.getTagId());
            if (tag != null) {
                tagNames.add(tag.getTagName());
            }
        }
        resp.setTagNames(tagNames);

        // 盒号/架位
        QueryWrapper<ArchiveBoxItem> bw = new QueryWrapper<>();
        bw.eq("archive_id", archive.getId());
        ArchiveBoxItem boxItem = archiveBoxItemMapper.selectOne(bw);
        if (boxItem != null) {
            ArchiveBox box = archiveBoxMapper.selectById(boxItem.getBoxId());
            if (box != null) {
                resp.setBoxNo(box.getBoxNo());
                if (box.getLocationId() != null) {
                    StorageLocation loc = storageLocationMapper.selectById(box.getLocationId());
                    if (loc != null) {
                        resp.setLocationCode(loc.getLocationCode());
                    }
                }
            }
        }

        return resp;
    }
}
