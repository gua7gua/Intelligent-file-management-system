package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.PageResult;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.LoanStatus;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
