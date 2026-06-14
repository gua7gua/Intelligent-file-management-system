package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ApprovalOpinionRequest;
import com.archive.dto.response.ApprovalDetailResponse;
import com.archive.dto.response.ApprovalResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.DestructionListMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批工作台服务。
 * 统一受理密级调整/开放调整/销毁三类审批，按类型在单事务内生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestMapper approvalMapper;
    private final ArchiveMapper archiveMapper;
    private final DestructionListMapper destructionListMapper;
    private final ArchiveChangeLogMapper changeLogMapper;
    private final AuditService auditService;

    // ==================== 13.1 查询审批单 ====================

    public PageResult<ApprovalResponse> listApprovals(
            String approvalType, String status, String keyword, int pageNo, int pageSize) {
        QueryWrapper<ApprovalRequest> w = new QueryWrapper<>();
        if (approvalType != null && !approvalType.isBlank()) {
            w.eq("approval_type", approvalType);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like("reason", keyword);
        }
        w.orderByDesc("submitted_at");

        Page<ApprovalRequest> page = approvalMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<ApprovalResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private ApprovalResponse toResponse(ApprovalRequest ap) {
        ApprovalResponse r = new ApprovalResponse();
        r.setId(ap.getId());
        r.setApprovalType(ap.getApprovalType() != null ? ap.getApprovalType().name() : null);
        r.setTargetType(ap.getTargetType());
        r.setTargetId(ap.getTargetId());
        r.setStatus(ap.getStatus() != null ? ap.getStatus().name() : null);
        r.setReason(ap.getReason());
        r.setSubmittedBy(ap.getSubmittedBy());
        r.setSubmittedAt(ap.getSubmittedAt());
        r.setApprovalOpinion(ap.getApprovalOpinion());
        r.setTargetSummary(resolveTargetSummary(ap));
        return r;
    }

    // ==================== 13.2 获取审批详情 ====================

    public ApprovalDetailResponse getApprovalDetail(Long approvalId) {
        ApprovalRequest ap = approvalMapper.selectById(approvalId);
        if (ap == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批单不存在");
        }
        ApprovalDetailResponse r = new ApprovalDetailResponse();
        r.setId(ap.getId());
        r.setApprovalType(ap.getApprovalType() != null ? ap.getApprovalType().name() : null);
        r.setTargetType(ap.getTargetType());
        r.setTargetId(ap.getTargetId());
        r.setEvidenceArchiveId(ap.getEvidenceArchiveId());
        r.setOldValue(ap.getOldValue());
        r.setNewValue(ap.getNewValue());
        r.setReason(ap.getReason());
        r.setStatus(ap.getStatus() != null ? ap.getStatus().name() : null);
        r.setSubmittedBy(ap.getSubmittedBy());
        r.setSubmittedAt(ap.getSubmittedAt());
        r.setApprovedBy(ap.getApprovedBy());
        r.setApprovedAt(ap.getApprovedAt());
        r.setApprovalOpinion(ap.getApprovalOpinion());
        r.setTargetSummary(resolveTargetSummary(ap));
        if (ap.getEvidenceArchiveId() != null) {
            Archive ev = archiveMapper.selectById(ap.getEvidenceArchiveId());
            if (ev != null) {
                r.setEvidenceArchiveNo(ev.getArchiveNo());
            }
        }
        return r;
    }

    private String resolveTargetSummary(ApprovalRequest ap) {
        if (ap.getTargetId() == null) {
            return null;
        }
        if ("archive".equals(ap.getTargetType())) {
            Archive a = archiveMapper.selectById(ap.getTargetId());
            return a != null ? a.getArchiveNo() : null;
        }
        if ("destruction_list".equals(ap.getTargetType())) {
            DestructionList l = destructionListMapper.selectById(ap.getTargetId());
            return l != null ? l.getListNo() : null;
        }
        return null;
    }

    // ==================== 13.3 审批通过 ====================

    @Transactional
    public ApprovalDetailResponse approve(Long approvalId, ApprovalOpinionRequest req) {
        ApprovalRequest ap = approvalMapper.selectById(approvalId);
        if (ap == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批单不存在");
        }
        if (ap.getStatus() != ApprovalStatus.pending) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "审批单非待审批状态");
        }

        Long uid = safeCurrentUserId();
        OffsetDateTime now = OffsetDateTime.now();

        switch (ap.getApprovalType()) {
            case security_adjust -> applySecurityAdjust(ap, uid, now);
            case open_adjust -> applyOpenAdjust(ap, uid, now);
            case destruction -> applyDestructionApprove(ap);
        }

        ap.setStatus(ApprovalStatus.approved);
        ap.setApprovedBy(uid);
        ap.setApprovedAt(now);
        ap.setApprovalOpinion(req.getOpinion());
        approvalMapper.updateById(ap);

        auditService.log("M08", "approve", "approval_request", approvalId,
                Map.of("type", ap.getApprovalType() != null ? ap.getApprovalType().name() : ""));
        return getApprovalDetail(approvalId);
    }

    private void applySecurityAdjust(ApprovalRequest ap, Long uid, OffsetDateTime now) {
        Archive a = loadEffectableArchive(ap);
        Integer oldLevel = a.getSecurityLevel();
        a.setSecurityLevel(parseIntOrNull(ap.getNewValue()));
        archiveMapper.updateById(a);
        writeChangeLog(a.getId(), "security_level",
                oldLevel != null ? String.valueOf(oldLevel) : null, ap.getNewValue(),
                ap.getReason(), "security_adjust", ap.getId(), uid, now);
    }

    private void applyOpenAdjust(ApprovalRequest ap, Long uid, OffsetDateTime now) {
        Archive a = loadEffectableArchive(ap);
        String oldStatus = a.getOpenStatus();
        a.setOpenStatus(ap.getNewValue());
        archiveMapper.updateById(a);
        writeChangeLog(a.getId(), "open_status", oldStatus, ap.getNewValue(),
                ap.getReason(), "open_adjust", ap.getId(), uid, now);
    }

    private void applyDestructionApprove(ApprovalRequest ap) {
        DestructionList list = destructionListMapper.selectById(ap.getTargetId());
        if (list == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "销毁清册不存在");
        }
        if (list.getStatus() != DestructionListStatus.pending_approval) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "销毁清册当前状态不允许审批生效");
        }
        list.setStatus(DestructionListStatus.pending_destroy);
        destructionListMapper.updateById(list);
    }

    /** 加载可生效的档案：必须存在且未销毁。 */
    private Archive loadEffectableArchive(ApprovalRequest ap) {
        Archive a = archiveMapper.selectById(ap.getTargetId());
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标档案不存在");
        }
        if (a.getLifecycleStatus() == LifecycleStatus.destroyed) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标档案已销毁，审批不可生效");
        }
        return a;
    }

    // ==================== 13.4 审批退回 ====================

    @Transactional
    public ApprovalDetailResponse reject(Long approvalId, ApprovalOpinionRequest req) {
        if (req.getOpinion() == null || req.getOpinion().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "退回意见必填");
        }
        ApprovalRequest ap = approvalMapper.selectById(approvalId);
        if (ap == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批单不存在");
        }
        if (ap.getStatus() != ApprovalStatus.pending) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "审批单非待审批状态");
        }

        ap.setStatus(ApprovalStatus.rejected);
        ap.setApprovedBy(safeCurrentUserId());
        ap.setApprovedAt(OffsetDateTime.now());
        ap.setApprovalOpinion(req.getOpinion());
        approvalMapper.updateById(ap);

        // 销毁清册退回可重新编辑
        if (ap.getApprovalType() == ApprovalType.destruction) {
            DestructionList list = destructionListMapper.selectById(ap.getTargetId());
            if (list != null && list.getStatus() == DestructionListStatus.pending_approval) {
                list.setStatus(DestructionListStatus.draft);
                destructionListMapper.updateById(list);
            }
        }

        auditService.log("M08", "reject", "approval_request", approvalId,
                Map.of("opinion", req.getOpinion()));
        return getApprovalDetail(approvalId);
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

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
