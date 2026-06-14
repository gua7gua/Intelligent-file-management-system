package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowApproveRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.archive.enums.BorrowStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.UserMapper;
import com.archive.util.BorrowNoUtil;
import com.archive.util.PdfGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 借阅管理主服务（M09）。
 * 状态机内联：applied→approved/rejected→voucher_issued→checked_out→returned/abnormal_return。
 * 角色强制与审计写入在此层完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRequestMapper borrowRequestMapper;
    private final ArchiveMapper archiveMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BorrowNoUtil borrowNoUtil;
    private final BorrowEligibilityChecker eligibilityChecker;
    private final PdfGenerator pdfGenerator;
    private final AuditService auditService;

    private static final String LOCATION_OF_ARCHIVE_SQL =
            "SELECT ab.box_no, sl.location_code FROM archive_box_items abi " +
            "JOIN archive_boxes ab ON ab.id = abi.box_id " +
            "JOIN storage_locations sl ON sl.id = ab.location_id " +
            "WHERE abi.archive_id = ? AND abi.deleted_at IS NULL LIMIT 1";

    // ==================== 11.7 提交借阅申请 ====================

    @Transactional
    public BorrowRequestResponse apply(BorrowApplyRequest req) {
        requireRole(RoleCode.internal_reader);
        long userId = AuthContext.getCurrentUserId();

        eligibilityChecker.checkBorrowable(req.getArchiveId());

        BorrowRequest b = new BorrowRequest();
        b.setRequestNo(borrowNoUtil.nextRequestNo());
        b.setArchiveId(req.getArchiveId());
        b.setBorrowerId(userId);
        b.setReason(req.getReason());
        b.setExpectedDays(req.getExpectedDays());
        b.setExpectedVisitAt(req.getExpectedVisitAt());
        b.setContactPhone(req.getContactPhone());
        b.setStatus(BorrowStatus.applied);
        borrowRequestMapper.insert(b);

        auditService.log("M09", "apply", "borrow_request", b.getId(),
                Map.of("archiveId", req.getArchiveId(), "requestNo", b.getRequestNo()));

        return toResponse(b, false, false);
    }

    // ==================== 11.8 查询我的借阅申请 ====================

    public PageResult<BorrowRequestResponse> listMine(BorrowRequestQuery query) {
        requireRole(RoleCode.internal_reader);
        long userId = AuthContext.getCurrentUserId();

        Page<BorrowRequest> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            w.eq("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            w.like("request_no", query.getKeyword());
        }
        if (Boolean.TRUE.equals(query.getOverdue())) {
            w.eq("status", BorrowStatus.checked_out.name())
             .isNotNull("due_at").lt("due_at", OffsetDateTime.now());
        }
        w.orderByDesc("created_at");

        Page<BorrowRequest> result = borrowRequestMapper.selectPage(page, w);
        List<BorrowRequestResponse> items = result.getRecords().stream()
                .map(b -> toResponse(b, false, false))
                .collect(Collectors.toList());
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    // ==================== 11.9 借阅申请详情（本人） ====================

    public BorrowRequestResponse getMine(Long requestId) {
        requireRole(RoleCode.internal_reader);
        BorrowRequest b = mustGet(requestId);
        if (!b.getBorrowerId().equals(AuthContext.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该借阅申请");
        }
        return toResponse(b, false, false);
    }

    // ==================== 12.3 审批借阅申请 ====================

    @Transactional
    public BorrowRequestResponse approve(Long requestId, BorrowApproveRequest req) {
        requireRole(RoleCode.back_archivist);
        long reviewer = AuthContext.getCurrentUserId();

        BorrowRequest b = mustGet(requestId);
        if (b.getStatus() != BorrowStatus.applied) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许审批");
        }

        // 审批前重新校验可借状态与盘点范围
        eligibilityChecker.checkBorrowable(b.getArchiveId());

        OffsetDateTime now = OffsetDateTime.now();
        b.setApprovedBy(reviewer);
        b.setApprovedAt(now);

        if (Boolean.TRUE.equals(req.getApproved())) {
            b.setStatus(BorrowStatus.approved);
            b.setRejectReason(null);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "approve", "borrow_request", b.getId(),
                    Map.of("opinion", req.getOpinion() != null ? req.getOpinion() : ""));
        } else {
            String reason = req.getOpinion();
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "拒绝时必须填写意见");
            }
            b.setStatus(BorrowStatus.rejected);
            b.setRejectReason(reason);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "reject", "borrow_request", b.getId(),
                    Map.of("rejectReason", reason));
        }
        return toResponse(b, true, false);
    }

    // ==================== 公共辅助 ====================

    private BorrowRequest mustGet(Long requestId) {
        BorrowRequest b = borrowRequestMapper.selectById(requestId);
        if (b == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "借阅申请不存在");
        }
        return b;
    }

    private void requireRole(RoleCode... allowed) {
        for (RoleCode r : allowed) {
            if (AuthContext.hasRole(r)) return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
    }

    private BorrowRequestResponse toResponse(BorrowRequest b, boolean withBorrower, boolean withLocation) {
        Archive archive = b.getArchiveId() == null ? null : archiveMapper.selectById(b.getArchiveId());
        BorrowRequestResponse.ArchiveSummary archiveSummary = null;
        if (archive != null) {
            archiveSummary = new BorrowRequestResponse.ArchiveSummary(
                    archive.getId(), archive.getArchiveNo(), archive.getTitle(),
                    archive.getCarrierStatus() != null ? archive.getCarrierStatus().name() : null);
        }

        BorrowRequestResponse.BorrowerSummary borrowerSummary = null;
        if (withBorrower && b.getBorrowerId() != null) {
            User u = userMapper.selectById(b.getBorrowerId());
            if (u != null) {
                String orgName = null;
                if (u.getOrganizationId() != null) {
                    Organization org = organizationMapper.selectById(u.getOrganizationId());
                    orgName = org != null ? org.getOrgName() : null;
                }
                borrowerSummary = new BorrowRequestResponse.BorrowerSummary(
                        u.getId(), u.getRealName(), u.getEmployeeNo(),
                        u.getDepartmentName(), orgName);
            }
        }

        BorrowRequestResponse.LocationSummary locationSummary =
                withLocation ? resolveLocation(b.getArchiveId()) : null;

        boolean overdue = b.getStatus() == BorrowStatus.checked_out
                && b.getDueAt() != null && b.getDueAt().isBefore(OffsetDateTime.now());

        return new BorrowRequestResponse(
                b.getId(), b.getRequestNo(), b.getStatus().name(), b.getReason(),
                b.getExpectedDays(), b.getExpectedVisitAt(), b.getContactPhone(),
                b.getDueAt(), overdue, b.getRejectReason(), b.getVoucherNo(),
                b.getVoucherIssuedAt(), b.getApprovedAt(), b.getApprovedBy(),
                b.getCheckedOutAt(), b.getReturnedAt(),
                b.getReturnCheckResult() != null ? b.getReturnCheckResult().name() : null,
                b.getReturnNote(), b.getCreatedAt(),
                archiveSummary, borrowerSummary, locationSummary);
    }

    private BorrowRequestResponse.LocationSummary resolveLocation(Long archiveId) {
        if (archiveId == null) return null;
        try {
            return jdbcTemplate.queryForObject(LOCATION_OF_ARCHIVE_SQL,
                    (rs, rowNum) -> new BorrowRequestResponse.LocationSummary(
                            rs.getString("box_no"), rs.getString("location_code")),
                    archiveId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
