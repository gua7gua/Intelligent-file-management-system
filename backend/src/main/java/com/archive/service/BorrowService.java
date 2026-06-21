package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowApproveRequest;
import com.archive.dto.request.BorrowCheckoutRequest;
import com.archive.dto.request.BorrowReturnRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.dto.response.BorrowSummaryItem;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.archive.enums.BorrowStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LoanStatus;
import com.archive.enums.ReturnCheckResult;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.UserMapper;
import com.archive.util.BorrowNoUtil;
import com.archive.util.PdfGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

    // ==================== 12.2.1 撤回借阅申请（仅本人 + applied 状态，撤回即删除） ====================

    @Transactional
    public void cancelRequest(Long requestId) {
        requireRole(RoleCode.internal_reader);
        BorrowRequest b = mustGet(requestId);
        if (!b.getBorrowerId().equals(AuthContext.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能撤回本人的借阅申请");
        }
        if (b.getStatus() != BorrowStatus.applied) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "仅待审批（已申请）状态可撤回");
        }
        borrowRequestMapper.deleteById(requestId);
        auditService.log("M09", "cancel_borrow", "borrow_request", requestId, Map.of());
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

        // 审批前重新校验可借状态与盘点范围（排除当前申请自身，避免复校误拦）
        eligibilityChecker.checkBorrowable(b.getArchiveId(), requestId);

        OffsetDateTime now = OffsetDateTime.now();
        b.setApprovedBy(reviewer);
        b.setApprovedAt(now);

        if (Boolean.TRUE.equals(req.getApproved())) {
            b.setStatus(BorrowStatus.approved);
            b.setRejectReason(null);
            // 审批通过即自动生成凭证号：借阅人「我的借阅申请」详情立即可见，前台出库时凭证号只读无需手填
            if (b.getVoucherNo() == null) {
                b.setVoucherNo(borrowNoUtil.nextVoucherNo());
                b.setVoucherIssuedAt(now);
            }
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "approve", "borrow_request", b.getId(),
                    Map.of("opinion", req.getOpinion() != null ? req.getOpinion() : "",
                            "voucherNo", b.getVoucherNo() != null ? b.getVoucherNo() : ""));
        } else {
            // 接口文档 §12.3：拒绝时 opinion 或 rejectReason 二选一必填
            String reason = req.getOpinion();
            if (reason == null || reason.isBlank()) {
                reason = req.getRejectReason();
            }
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "拒绝时必须填写意见");
            }
            b.setStatus(BorrowStatus.rejected);
            b.setRejectReason(reason);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "reject", "borrow_request", b.getId(),
                    Map.of("rejectReason", reason));
        }
        return toResponse(b, true, true, true);
    }

    // ==================== 11.10 导出借阅凭证 ====================

    @Transactional
    public byte[] exportVoucher(Long requestId) {
        requireRole(RoleCode.internal_reader);
        BorrowRequest b = mustGet(requestId);
        if (!b.getBorrowerId().equals(AuthContext.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权导出该借阅凭证");
        }

        BorrowStatus st = b.getStatus();
        // returned 也允许补打凭证（归还后作历史/报销凭据）；此时 voucher_no 已发，仅补打不改状态
        if (st != BorrowStatus.approved && st != BorrowStatus.voucher_issued
                && st != BorrowStatus.checked_out && st != BorrowStatus.returned) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "申请尚未审批通过，无法导出凭证");
        }

        // 凭证号在审批通过时已生成；此处为首次打印凭证 PDF：把状态从 approved 推进到 voucher_issued
        boolean firstIssue = b.getStatus() == BorrowStatus.approved;
        if (firstIssue) {
            b.setStatus(BorrowStatus.voucher_issued);
            borrowRequestMapper.updateById(b);
            auditService.log("M09", "issue_voucher", "borrow_request", b.getId(),
                    Map.of("voucherNo", b.getVoucherNo(), "firstIssue", true));
        } else {
            auditService.log("M09", "issue_voucher", "borrow_request", b.getId(),
                    Map.of("voucherNo", b.getVoucherNo(), "firstIssue", false));
        }

        Archive archive = archiveMapper.selectById(b.getArchiveId());
        User borrower = userMapper.selectById(b.getBorrowerId());
        Organization org = (borrower != null && borrower.getOrganizationId() != null)
                ? organizationMapper.selectById(borrower.getOrganizationId()) : null;
        // 跨单位纸质借阅：档案所属单位 ≠ 借阅人单位 → 凭证改由档案馆代原单位盖章
        boolean crossOrg = archive != null && archive.getOrganizationId() != null
                && borrower != null && borrower.getOrganizationId() != null
                && !archive.getOrganizationId().equals(borrower.getOrganizationId());
        String archiveOrgName = null;
        if (crossOrg) {
            Organization archiveOrg = organizationMapper.selectById(archive.getOrganizationId());
            archiveOrgName = archiveOrg != null ? archiveOrg.getOrgName() : null;
        }
        return pdfGenerator.generateBorrowVoucherPdf(b, archive, borrower, org, crossOrg, archiveOrgName);
    }

    // ==================== 12.4 核验凭证并确认出库 ====================

    @Transactional
    public BorrowRequestResponse checkout(Long requestId, BorrowCheckoutRequest req) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);
        long operator = AuthContext.getCurrentUserId();

        BorrowRequest b = mustGet(requestId);
        BorrowStatus st = b.getStatus();
        if (st != BorrowStatus.approved && st != BorrowStatus.voucher_issued) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许出库");
        }
        if (b.getVoucherNo() == null || !b.getVoucherNo().equals(req.getVoucherNo())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "凭证号不匹配");
        }
        // 应还时间：前端未填则按借阅时长自动计算（出库时刻 + expectedDays 天），再校验晚于当前时间
        OffsetDateTime dueAt = req.getDueAt();
        if (dueAt == null && b.getExpectedDays() != null && b.getExpectedDays() > 0) {
            dueAt = OffsetDateTime.now().plusDays(b.getExpectedDays());
        }
        if (dueAt == null || !dueAt.isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "应还时间必须晚于当前时间");
        }

        // 出库复校可借状态与盘点范围（排除当前申请自身，避免复校误拦）
        eligibilityChecker.checkBorrowable(b.getArchiveId(), requestId);

        OffsetDateTime now = OffsetDateTime.now();
        b.setCheckedOutBy(operator);
        b.setCheckedOutAt(now);
        b.setDueAt(dueAt);
        b.setStatus(BorrowStatus.checked_out);
        borrowRequestMapper.updateById(b);

        // 条件更新档案为借出中（充当乐观锁，并发时 updated=0 即冲突）
        Archive patch = new Archive();
        patch.setLoanStatus(LoanStatus.on_loan);
        UpdateWrapper<Archive> uw = new UpdateWrapper<>();
        uw.eq("id", b.getArchiveId()).eq("loan_status", LoanStatus.available.name());
        int updated = archiveMapper.update(patch, uw);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案状态已变更，出库失败");
        }

        auditService.log("M09", "checkout", "borrow_request", b.getId(),
                Map.of("voucherNo", req.getVoucherNo(),
                        "dueAt", String.valueOf(dueAt),
                        "note", req.getNote() != null ? req.getNote() : ""));

        return toResponse(b, true, true, true);
    }

    // ==================== 12.5 确认归还 ====================

    @Transactional
    public BorrowRequestResponse returnBorrow(Long requestId, BorrowReturnRequest req) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);
        long operator = AuthContext.getCurrentUserId();

        BorrowRequest b = mustGet(requestId);
        if (b.getStatus() != BorrowStatus.checked_out) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有已出库申请可归还");
        }

        OffsetDateTime now = OffsetDateTime.now();
        b.setReturnedBy(operator);
        b.setReturnedAt(now);
        b.setReturnCheckResult(req.getReturnCheckResult());
        b.setReturnNote(req.getReturnNote());

        boolean abnormal = req.getReturnCheckResult() != ReturnCheckResult.normal;
        b.setStatus(abnormal ? BorrowStatus.abnormal_return : BorrowStatus.returned);
        borrowRequestMapper.updateById(b);

        // 档案恢复可借；异常归还时实体状态置 damaged（条件更新充当乐观锁）
        Archive patch = new Archive();
        patch.setLoanStatus(LoanStatus.available);
        if (abnormal) {
            patch.setConditionStatus(ConditionStatus.damaged);
        }
        UpdateWrapper<Archive> uw = new UpdateWrapper<>();
        uw.eq("id", b.getArchiveId()).eq("loan_status", LoanStatus.on_loan.name());
        int updated = archiveMapper.update(patch, uw);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案状态已变更，归还失败");
        }

        auditService.log("M09", "return", "borrow_request", b.getId(),
                Map.of("returnCheckResult", req.getReturnCheckResult().name(), "abnormal", abnormal));

        return toResponse(b, true, true, true);
    }

    // ==================== 12.1 管理端查询借阅申请 ====================

    public PageResult<BorrowRequestResponse> adminList(BorrowRequestQuery query) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);

        Page<BorrowRequest> page = new Page<>(query.getPageNo(), query.getPageSize());
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.isNull("deleted_at");
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            w.eq("status", query.getStatus());
        }
        if (query.getBorrowerKeyword() != null && !query.getBorrowerKeyword().isBlank()) {
            String kw = "%" + query.getBorrowerKeyword() + "%";
            w.apply("borrower_id IN (SELECT id FROM users WHERE deleted_at IS NULL AND "
                    + "(real_name LIKE {0} OR login_name LIKE {0} OR employee_no LIKE {0}))", kw);
        }
        if (query.getArchiveKeyword() != null && !query.getArchiveKeyword().isBlank()) {
            String kw = "%" + query.getArchiveKeyword() + "%";
            w.apply("archive_id IN (SELECT id FROM archives WHERE deleted_at IS NULL AND "
                    + "(title LIKE {0} OR archive_no LIKE {0}))", kw);
        }
        if (Boolean.TRUE.equals(query.getOverdue())) {
            w.eq("status", BorrowStatus.checked_out.name())
             .isNotNull("due_at").lt("due_at", OffsetDateTime.now());
        }
        w.orderByDesc("created_at");

        Page<BorrowRequest> result = borrowRequestMapper.selectPage(page, w);
        List<BorrowRequestResponse> items = result.getRecords().stream()
                .map(b -> toResponse(b, true, false, true))
                .collect(Collectors.toList());
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), result.getTotal());
    }

    // ==================== 12.2 管理端申请详情 ====================

    public BorrowRequestResponse adminGet(Long requestId) {
        requireRole(RoleCode.front_archivist, RoleCode.back_archivist);
        BorrowRequest b = mustGet(requestId);
        return toResponse(b, true, true, true);
    }

    // ==================== 11.1 内部工作台借阅摘要 ====================

    public List<BorrowSummaryItem> dashboardMine(long userId, int limit) {
        Page<BorrowRequest> page = new Page<>(1, limit);
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).isNull("deleted_at").orderByDesc("created_at");
        return borrowRequestMapper.selectPage(page, w).getRecords().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<BorrowSummaryItem> dashboardCurrent(long userId, int limit) {
        Page<BorrowRequest> page = new Page<>(1, limit);
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).eq("status", BorrowStatus.checked_out.name())
         .isNull("deleted_at").orderByAsc("due_at");
        return borrowRequestMapper.selectPage(page, w).getRecords().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<BorrowSummaryItem> dashboardOverdue(long userId, int limit) {
        Page<BorrowRequest> page = new Page<>(1, limit);
        QueryWrapper<BorrowRequest> w = new QueryWrapper<>();
        w.eq("borrower_id", userId).eq("status", BorrowStatus.checked_out.name())
         .isNull("deleted_at").isNotNull("due_at").lt("due_at", OffsetDateTime.now())
         .orderByAsc("due_at");
        return borrowRequestMapper.selectPage(page, w).getRecords().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    private BorrowSummaryItem toSummary(BorrowRequest b) {
        String archiveNo = null;
        String title = null;
        if (b.getArchiveId() != null) {
            Archive a = archiveMapper.selectById(b.getArchiveId());
            if (a != null) {
                archiveNo = a.getArchiveNo();
                title = a.getTitle();
            }
        }
        return new BorrowSummaryItem(b.getRequestNo(), b.getArchiveId(), archiveNo, title,
                b.getStatus().name(), b.getDueAt(), b.getCreatedAt());
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
        return toResponse(b, withBorrower, withLocation, false);
    }

    /**
     * 构造响应。withCheckInfo=true 时计算并填充可借检查四项（载体/生命周期/借阅状态/盘点范围），
     * 仅管理端 §12.1/§12.2 列表与详情使用。
     */
    private BorrowRequestResponse toResponse(BorrowRequest b, boolean withBorrower,
                                             boolean withLocation, boolean withCheckInfo) {
        Archive archive = b.getArchiveId() == null ? null : archiveMapper.selectById(b.getArchiveId());
        BorrowRequestResponse.ArchiveSummary archiveSummary = null;
        if (archive != null) {
            archiveSummary = new BorrowRequestResponse.ArchiveSummary(
                    archive.getId(), archive.getArchiveNo(), archive.getTitle(),
                    archive.getCarrierStatus() != null ? archive.getCarrierStatus().name() : null,
                    archive.getLifecycleStatus() != null ? archive.getLifecycleStatus().name() : null,
                    archive.getLoanStatus() != null ? archive.getLoanStatus().name() : null,
                    archive.getConditionStatus() != null ? archive.getConditionStatus().name() : null);
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

        String checkCarrier = null;
        String checkLifecycle = null;
        String checkLoan = null;
        String checkInventory = null;
        if (withCheckInfo && archive != null) {
            checkCarrier = describeCarrier(archive.getCarrierStatus());
            checkLifecycle = describeLifecycle(archive.getLifecycleStatus());
            checkLoan = describeLoan(archive.getLoanStatus());
            checkInventory = describeInventory(archive);
        }

        return new BorrowRequestResponse(
                b.getId(), b.getRequestNo(), b.getStatus().name(), b.getReason(),
                b.getExpectedDays(), b.getExpectedVisitAt(), b.getContactPhone(),
                b.getDueAt(), overdue, b.getRejectReason(), b.getVoucherNo(),
                b.getVoucherIssuedAt(), b.getApprovedAt(), b.getApprovedBy(),
                b.getCheckedOutAt(), b.getReturnedAt(),
                b.getReturnCheckResult() != null ? b.getReturnCheckResult().name() : null,
                b.getReturnNote(), b.getCreatedAt(),
                checkCarrier, checkLifecycle, checkLoan, checkInventory,
                archiveSummary, borrowerSummary, locationSummary);
    }

    /** 载体可借检查文案：纸质/纸质+电子 可借，纯电子不可借。 */
    private String describeCarrier(com.archive.enums.CarrierStatus c) {
        if (c == null) return "未知";
        switch (c) {
            case paper: return "纸质（可借）";
            case paper_electronic: return "纸质+电子（可借）";
            case electronic: return "纯电子（不可借）";
            default: return c.name();
        }
    }

    /** 生命周期检查文案：normal 可借，其他不可借。 */
    private String describeLifecycle(com.archive.enums.LifecycleStatus s) {
        if (s == null) return "未知";
        switch (s) {
            case normal: return "正常（可借）";
            case pending_shelf: return "待上架（不可借）";
            case pending_destruction: return "待销毁（不可借）";
            case destroyed: return "已销毁（不可借）";
            default: return s.name();
        }
    }

    /** 借阅状态检查文案：available 可借，on_loan 已借出。 */
    private String describeLoan(com.archive.enums.LoanStatus s) {
        if (s == null) return "未知";
        switch (s) {
            case available: return "可借";
            case on_loan: return "已借出";
            default: return s.name();
        }
    }

    /** 盘点范围检查文案：命中运行中盘点返回"盘点中"，否则"未盘点/正常"。 */
    private String describeInventory(Archive archive) {
        try {
            Long roomId = jdbcTemplate.queryForObject(
                    "SELECT sl.room_id FROM archive_box_items abi " +
                    "JOIN archive_boxes ab ON ab.id = abi.box_id " +
                    "JOIN storage_locations sl ON sl.id = ab.location_id " +
                    "WHERE abi.archive_id = ? AND abi.deleted_at IS NULL LIMIT 1",
                    Long.class, archive.getId());
            if (roomId == null) return "未入盒（不适用）";
            Integer running = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inventory_tasks " +
                    "WHERE status = 'running' AND room_id = ? AND category_id = ?",
                    Integer.class, roomId, archive.getCategoryId());
            return (running != null && running > 0) ? "盘点中（暂停借阅）" : "正常";
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return "未入盒（不适用）";
        }
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
