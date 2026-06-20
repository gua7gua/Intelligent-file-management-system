package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.dto.request.OpenAdjustRequest;
import com.archive.entity.Archive;
import com.archive.entity.ApprovalRequest;
import com.archive.enums.LifecycleStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.ArchiveTagMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.FondsMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.StorageLocationMapper;
import com.archive.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ArchiveService 单测，重点覆盖「公开的涉密档案」防护（createOpenAdjustment 拒绝规则）。
 */
class ArchiveServiceTest {

    private ArchiveService service;
    private ArchiveMapper archiveMapper;
    private ArchiveFileMapper archiveFileMapper;
    private ArchiveTagMapper archiveTagMapper;
    private TagMapper tagMapper;
    private ArchiveChangeLogMapper archiveChangeLogMapper;
    private ApprovalRequestMapper approvalRequestMapper;
    private IntakeItemMapper intakeItemMapper;
    private IntakeBatchMapper intakeBatchMapper;
    private OrganizationMapper organizationMapper;
    private CategoryMapper categoryMapper;
    private FondsMapper fondsMapper;
    private ArchiveBoxItemMapper archiveBoxItemMapper;
    private ArchiveBoxMapper archiveBoxMapper;
    private StorageLocationMapper storageLocationMapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        archiveTagMapper = mock(ArchiveTagMapper.class);
        tagMapper = mock(TagMapper.class);
        archiveChangeLogMapper = mock(ArchiveChangeLogMapper.class);
        approvalRequestMapper = mock(ApprovalRequestMapper.class);
        intakeItemMapper = mock(IntakeItemMapper.class);
        intakeBatchMapper = mock(IntakeBatchMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        fondsMapper = mock(FondsMapper.class);
        archiveBoxItemMapper = mock(ArchiveBoxItemMapper.class);
        archiveBoxMapper = mock(ArchiveBoxMapper.class);
        storageLocationMapper = mock(StorageLocationMapper.class);
        auditService = mock(AuditService.class);
        // 构造参数顺序与 ArchiveService 字段声明顺序一致（@RequiredArgsConstructor）
        service = new ArchiveService(archiveMapper, archiveFileMapper, archiveTagMapper, tagMapper,
                archiveChangeLogMapper, approvalRequestMapper, intakeItemMapper, intakeBatchMapper,
                organizationMapper, categoryMapper, fondsMapper, archiveBoxItemMapper, archiveBoxMapper,
                storageLocationMapper, auditService);
    }

    @Test
    void createOpenAdjustment_涉密档案设公开_拒绝抛校验失败() {
        Archive a = new Archive();
        a.setId(10L);
        a.setSecurityLevel(2); // 秘密，高于非密
        a.setOpenStatus("closed");
        when(archiveMapper.selectById(10L)).thenReturn(a);

        OpenAdjustRequest req = new OpenAdjustRequest();
        req.setNewOpenStatus("open");
        req.setEvidenceArchiveNo("ARC-000018");
        req.setReason("尝试公开涉密档案");

        // 校验在校验链早期（AuthContext 调用之前）即抛出
        assertThatThrownBy(() -> service.createOpenAdjustment(10L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        // 被拒，不生成审批单
        verify(approvalRequestMapper, never()).insert(any(ApprovalRequest.class));
    }

    @Test
    void createOpenAdjustment_非密档案设公开_放行() {
        Archive target = new Archive();
        target.setId(10L);
        target.setSecurityLevel(0); // 非密
        target.setOpenStatus("closed");
        target.setOrganizationId(1L);
        when(archiveMapper.selectById(10L)).thenReturn(target);

        // 凭证档案：同组织、未销毁；validateEvidenceArchive 与 findArchiveByNo 均走 selectOne
        Archive evidence = new Archive();
        evidence.setId(18L);
        evidence.setArchiveNo("ARC-000018");
        evidence.setLifecycleStatus(LifecycleStatus.normal);
        evidence.setOrganizationId(1L);
        when(archiveMapper.selectOne(any())).thenReturn(evidence);

        // 无 pending 重复审批
        when(approvalRequestMapper.selectCount(any())).thenReturn(0L);

        OpenAdjustRequest req = new OpenAdjustRequest();
        req.setNewOpenStatus("open");
        req.setEvidenceArchiveNo("ARC-000018");
        req.setReason("非密档案可公开");

        // createOpenAdjustment 内部调 AuthContext.getCurrentUserId()（直读 SaToken，测试无上下文），
        // 用 mockStatic 桩掉，避免 SaTokenContext 未初始化异常
        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getCurrentUserId).thenReturn(1L);
            service.createOpenAdjustment(10L, req);
        }

        ArgumentCaptor<ApprovalRequest> cap = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvalRequestMapper).insert(cap.capture());
        assertThat(cap.getValue().getNewValue()).isEqualTo("open");
    }
}
