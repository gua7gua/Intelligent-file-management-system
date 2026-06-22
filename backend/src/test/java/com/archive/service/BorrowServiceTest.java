package com.archive.service;

import com.archive.common.AuthContext;
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
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    private BorrowService service;
    private BorrowRequestMapper borrowRequestMapper;
    private ArchiveMapper archiveMapper;
    private UserMapper userMapper;
    private OrganizationMapper organizationMapper;
    private JdbcTemplate jdbcTemplate;
    private BorrowNoUtil borrowNoUtil;
    private BorrowEligibilityChecker eligibilityChecker;
    private PdfGenerator pdfGenerator;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        borrowRequestMapper = mock(BorrowRequestMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        userMapper = mock(UserMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        borrowNoUtil = mock(BorrowNoUtil.class);
        eligibilityChecker = mock(BorrowEligibilityChecker.class);
        pdfGenerator = mock(PdfGenerator.class);
        auditService = mock(AuditService.class);

        service = new BorrowService(borrowRequestMapper, archiveMapper, userMapper,
                organizationMapper, jdbcTemplate, borrowNoUtil,
                eligibilityChecker, pdfGenerator, auditService);
    }

    private void asRole(RoleCode role, long userId, Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            if (role != null) {
                a.when(() -> AuthContext.hasRole(role)).thenReturn(true);
            }
            a.when(AuthContext::getCurrentUserId).thenReturn(userId);
            body.run();
        }
    }

    private Archive borrowableArchive(Long id) {
        Archive a = new Archive();
        a.setId(id);
        a.setArchiveNo("ARC-000002");
        a.setTitle("测试档案");
        a.setCarrierStatus(CarrierStatus.paper);
        a.setLifecycleStatus(LifecycleStatus.normal);
        a.setLoanStatus(LoanStatus.available);
        a.setConditionStatus(ConditionStatus.normal);
        a.setCategoryId(3);
        return a;
    }

    private BorrowRequest sampleRequest(Long id, String requestNo, Long borrowerId, BorrowStatus status) {
        BorrowRequest b = new BorrowRequest();
        b.setId(id);
        b.setRequestNo(requestNo);
        b.setBorrowerId(borrowerId);
        b.setArchiveId(2L);
        b.setStatus(status);
        b.setReason("核查");
        b.setExpectedDays(7);
        return b;
    }

    @Test
    void apply_内部查阅者提交申请成功() {
        when(borrowNoUtil.nextRequestNo()).thenReturn("BRW-000001");
        when(borrowRequestMapper.insert(any(BorrowRequest.class))).thenAnswer(inv -> {
            ((BorrowRequest) inv.getArgument(0)).setId(100L);
            return 1;
        });
        when(archiveMapper.selectById(2L)).thenReturn(borrowableArchive(2L));

        BorrowApplyRequest req = new BorrowApplyRequest();
        req.setArchiveId(2L);
        req.setReason("财政核查");
        req.setExpectedDays(7);

        asRole(RoleCode.internal_reader, 4L, () -> {
            BorrowRequestResponse resp = service.apply(req);
            assertThat(resp.getId()).isEqualTo(100L);
            assertThat(resp.getRequestNo()).isEqualTo("BRW-000001");
            assertThat(resp.getStatus()).isEqualTo("applied");
            assertThat(resp.getBorrower()).isNull();
        });

        verify(borrowRequestMapper).insert(any(BorrowRequest.class));
        verify(eligibilityChecker).checkBorrowable(2L);
        verify(auditService).log(eq("M09"), eq("apply"), eq("borrow_request"), eq(100L), any());
    }

    @Test
    void apply_非内部查阅者抛FORBIDDEN() {
        BorrowApplyRequest req = new BorrowApplyRequest();
        req.setArchiveId(2L);
        req.setReason("财政核查");
        req.setExpectedDays(7);

        asRole(null, 4L, () ->
                assertThatThrownBy(() -> service.apply(req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    @Test
    void listMine_按当前用户过滤分页且内部端不带borrower() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        Page<BorrowRequest> page = new Page<>(1, 20);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowRequestQuery q = new BorrowRequestQuery();
        asRole(RoleCode.internal_reader, 4L, () -> {
            PageResult<BorrowRequestResponse> result = service.listMine(q);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getRequestNo()).isEqualTo("BRW-000001");
            assertThat(result.getRecords().get(0).getBorrower()).isNull();
        });
    }

    @Test
    void getMine_本人申请可见() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThat(service.getMine(1L).getRequestNo()).isEqualTo("BRW-000001"));
    }

    @Test
    void getMine_非本人申请抛FORBIDDEN() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 9L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.getMine(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无权查看"));
    }

    @Test
    void getMine_申请不存在抛NOT_FOUND() {
        when(borrowRequestMapper.selectById(1L)).thenReturn(null);
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.getMine(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("不存在"));
    }

    // ---- 12.3 approve ----

    @Test
    void approve_通过则状态变approved并记录处理人() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(true);
        req.setOpinion("同意借阅7天");

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.approve(1L, req);
            assertThat(resp.getStatus()).isEqualTo("approved");
            assertThat(resp.getApprovedBy()).isEqualTo(2L);
            assertThat(resp.getRejectReason()).isNull();
        });
    }

    @Test
    void approve_拒绝则状态变rejected并写rejectReason() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(false);
        req.setOpinion("档案盘点中");

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.approve(1L, req);
            assertThat(resp.getStatus()).isEqualTo("rejected");
            assertThat(resp.getRejectReason()).isEqualTo("档案盘点中");
        });
    }

    @Test
    void approve_拒绝未填意见抛VALIDATION_FAILED() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(false);

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.approve(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("意见"));
    }

    @Test
    void approve_非applied状态抛BUSINESS_CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(true);

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.approve(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("当前状态"));
    }

    @Test
    void approve_非后台管理员抛FORBIDDEN() {
        BorrowApproveRequest req = new BorrowApproveRequest();
        req.setApproved(true);

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.approve(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    // ---- 11.10 exportVoucher ----

    @Test
    void exportVoucher_首次导出生成凭证号并置voucher_issued() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.approved);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(borrowNoUtil.nextVoucherNo()).thenReturn("VCH-000001");
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(userMapper.selectById(any())).thenReturn(new com.archive.entity.User());
        when(pdfGenerator.generateBorrowVoucherPdf(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        asRole(RoleCode.internal_reader, 4L, () -> {
            byte[] pdf = service.exportVoucher(1L);
            assertThat(pdf).isNotEmpty();
        });

        ArgumentCaptor<BorrowRequest> cap = ArgumentCaptor.forClass(BorrowRequest.class);
        verify(borrowRequestMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(BorrowStatus.voucher_issued);
        assertThat(cap.getValue().getVoucherNo()).isEqualTo("VCH-000001");
    }

    @Test
    void exportVoucher_重复导出复用凭证号不更新() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(userMapper.selectById(any())).thenReturn(new com.archive.entity.User());
        when(pdfGenerator.generateBorrowVoucherPdf(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new byte[]{1});

        asRole(RoleCode.internal_reader, 4L, () -> service.exportVoucher(1L));

        verify(borrowNoUtil, never()).nextVoucherNo();
        verify(borrowRequestMapper, never()).updateById(any(BorrowRequest.class));
    }

    @Test
    void exportVoucher_未审批通过抛BUSINESS_CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.exportVoucher(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("审批通过"));
    }

    @Test
    void exportVoucher_非本人抛FORBIDDEN() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 9L, BorrowStatus.approved);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.exportVoucher(1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无权"));
    }

    // ---- 12.4 checkout ----

    @Test
    void checkout_凭证匹配则出库成功档案置on_loan() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(archiveMapper.update(any(com.archive.entity.Archive.class), any())).thenReturn(1);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.checkout(1L, req);
            assertThat(resp.getStatus()).isEqualTo("checked_out");
        });
        verify(archiveMapper).update(any(com.archive.entity.Archive.class), any());
    }

    @Test
    void checkout_凭证号不匹配抛CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-999999");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("凭证号不匹配"));
    }

    @Test
    void checkout_应还时间不晚于当前抛VALIDATION_FAILED() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.voucher_issued);
        b.setVoucherNo("VCH-000001");
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().minusDays(1));

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("应还时间"));
    }

    @Test
    void checkout_非可出库状态抛CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("当前状态"));
    }

    @Test
    void checkout_无权限角色抛FORBIDDEN() {
        BorrowCheckoutRequest req = new BorrowCheckoutRequest();
        req.setVoucherNo("VCH-000001");
        req.setDueAt(OffsetDateTime.now().plusDays(7));

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.checkout(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    // ---- 12.5 returnBorrow ----

    @Test
    void returnBorrow_正常归还则状态returned且档案恢复可借() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.update(any(Archive.class), any())).thenReturn(1);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.normal);

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.returnBorrow(1L, req);
            assertThat(resp.getStatus()).isEqualTo("returned");
        });

        ArgumentCaptor<Archive> cap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).update(cap.capture(), any());
        assertThat(cap.getValue().getLoanStatus()).isEqualTo(LoanStatus.available);
        assertThat(cap.getValue().getConditionStatus()).isNull(); // 正常归还不改实体状态
    }

    @Test
    void returnBorrow_异常归还则状态abnormal_return且档案置damaged() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.update(any(Archive.class), any())).thenReturn(1);

        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.damaged);
        req.setReturnNote("缺页2页");

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.returnBorrow(1L, req);
            assertThat(resp.getStatus()).isEqualTo("abnormal_return");
            assertThat(resp.getReturnCheckResult()).isEqualTo("damaged");
        });

        ArgumentCaptor<Archive> cap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).update(cap.capture(), any());
        assertThat(cap.getValue().getConditionStatus()).isEqualTo(ConditionStatus.damaged);
    }

    @Test
    void returnBorrow_非checked_out状态抛CONFLICT() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);

        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.normal);

        asRole(RoleCode.back_archivist, 2L, () ->
                assertThatThrownBy(() -> service.returnBorrow(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("已出库"));
    }

    @Test
    void returnBorrow_无权限角色抛FORBIDDEN() {
        BorrowReturnRequest req = new BorrowReturnRequest();
        req.setReturnCheckResult(ReturnCheckResult.normal);

        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.returnBorrow(1L, req))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    // ---- 12.1 adminList ----

    @Test
    void adminList_管理端响应带borrower摘要() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        Page<BorrowRequest> page = new Page<>(1, 20);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(), any())).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        com.archive.entity.User u = new com.archive.entity.User();
        u.setId(4L);
        u.setRealName("小李");
        when(userMapper.selectById(4L)).thenReturn(u);

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestQuery q = new BorrowRequestQuery();
            PageResult<BorrowRequestResponse> r = service.adminList(q);
            assertThat(r.getRecords()).hasSize(1);
            assertThat(r.getRecords().get(0).getBorrower()).isNotNull();
            assertThat(r.getRecords().get(0).getBorrower().getRealName()).isEqualTo("小李");
        });
    }

    @Test
    void adminList_非管理端角色抛FORBIDDEN() {
        asRole(RoleCode.internal_reader, 4L, () ->
                assertThatThrownBy(() -> service.adminList(new BorrowRequestQuery()))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("无操作权限"));
    }

    // ---- 12.2 adminGet ----

    @Test
    void adminGet_带盒位架位location() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        when(borrowRequestMapper.selectById(1L)).thenReturn(b);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));
        when(userMapper.selectById(any())).thenReturn(new com.archive.entity.User());
        BorrowRequestResponse.LocationSummary loc =
                new BorrowRequestResponse.LocationSummary("BOX-000001", "401-01-02-03");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(loc);

        asRole(RoleCode.back_archivist, 2L, () -> {
            BorrowRequestResponse resp = service.adminGet(1L);
            assertThat(resp.getLocation()).isNotNull();
            assertThat(resp.getLocation().getBoxNo()).isEqualTo("BOX-000001");
        });
    }

    // ---- 工作台 dashboard 方法（不需角色校验，由 SearchService 已认证用户调用） ----

    @Test
    void dashboardMine_返回用户最近申请摘要() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.applied);
        Page<BorrowRequest> page = new Page<>(1, 5);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(), any())).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        List<BorrowSummaryItem> items = service.dashboardMine(4L, 5);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getRequestNo()).isEqualTo("BRW-000001");
        assertThat(items.get(0).getTitle()).isEqualTo("测试档案");
    }

    @Test
    void dashboardOverdue_只返回逾期借阅() {
        BorrowRequest b = sampleRequest(1L, "BRW-000001", 4L, BorrowStatus.checked_out);
        b.setDueAt(java.time.OffsetDateTime.now().minusDays(2));
        Page<BorrowRequest> page = new Page<>(1, 5);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(borrowRequestMapper.selectPage(any(), any())).thenReturn(page);
        when(archiveMapper.selectById(any())).thenReturn(borrowableArchive(2L));

        List<BorrowSummaryItem> items = service.dashboardOverdue(4L, 5);
        assertThat(items).hasSize(1);
    }
}
