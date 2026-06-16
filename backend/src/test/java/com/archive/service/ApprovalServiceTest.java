package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.ApprovalOpinionRequest;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.ApprovalType;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.mapper.FondsMapper;
import com.archive.mapper.OrganizationMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApprovalServiceTest {

    private ApprovalService service;
    private ApprovalRequestMapper approvalMapper;
    private ArchiveMapper archiveMapper;
    private DestructionListMapper destructionListMapper;
    private DestructionItemMapper destructionItemMapper;
    private ArchiveChangeLogMapper changeLogMapper;
    private UserMapper userMapper;
    private CategoryMapper categoryMapper;
    private OrganizationMapper organizationMapper;
    private FondsMapper fondsMapper;
    private AppraisalBatchMapper appraisalBatchMapper;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        approvalMapper = mock(ApprovalRequestMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        destructionListMapper = mock(DestructionListMapper.class);
        destructionItemMapper = mock(DestructionItemMapper.class);
        changeLogMapper = mock(ArchiveChangeLogMapper.class);
        userMapper = mock(UserMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        fondsMapper = mock(FondsMapper.class);
        appraisalBatchMapper = mock(AppraisalBatchMapper.class);
        auditService = mock(AuditService.class);
        service = new ApprovalService(approvalMapper, archiveMapper, destructionListMapper,
                destructionItemMapper, changeLogMapper, userMapper, categoryMapper,
                organizationMapper, fondsMapper, appraisalBatchMapper, auditService);
    }

    @Test
    void approve_销毁审批通过置清册待销毁() {
        ApprovalRequest ap = pending(ApprovalType.destruction, "destruction_list", 1L);
        when(approvalMapper.selectById(7L)).thenReturn(ap);
        DestructionList list = new DestructionList();
        list.setId(1L);
        list.setStatus(DestructionListStatus.pending_approval);
        list.setListNo("DES-000001");
        when(destructionListMapper.selectById(1L)).thenReturn(list);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest();
        req.setOpinion("同意");
        service.approve(7L, req);

        ArgumentCaptor<DestructionList> cap = ArgumentCaptor.forClass(DestructionList.class);
        verify(destructionListMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(DestructionListStatus.pending_destroy);
        verify(approvalMapper).updateById(any(ApprovalRequest.class));
        verify(auditService).log(eq("M08"), eq("approve"), eq("approval_request"), eq(7L), any());
    }

    @Test
    void approve_密级调整生效档案并写变更日志() {
        ApprovalRequest ap = pending(ApprovalType.security_adjust, "archive", 10L);
        ap.setOldValue("1");
        ap.setNewValue("2");
        when(approvalMapper.selectById(7L)).thenReturn(ap);
        Archive a = new Archive();
        a.setId(10L);
        a.setArchiveNo("ARC-000010");
        a.setSecurityLevel(1);
        a.setLifecycleStatus(LifecycleStatus.normal);
        when(archiveMapper.selectById(10L)).thenReturn(a);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest();
        req.setOpinion("同意调整");
        service.approve(7L, req);

        ArgumentCaptor<Archive> archiveCap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).updateById(archiveCap.capture());
        assertThat(archiveCap.getValue().getSecurityLevel()).isEqualTo(2);
        ArgumentCaptor<ArchiveChangeLog> logCap = ArgumentCaptor.forClass(ArchiveChangeLog.class);
        verify(changeLogMapper).insert(logCap.capture());
        assertThat(logCap.getValue().getFieldName()).isEqualTo("security_level");
        assertThat(logCap.getValue().getApprovalRequestId()).isEqualTo(7L);
    }

    @Test
    void approve_非待审批抛冲突() {
        ApprovalRequest ap = new ApprovalRequest();
        ap.setId(7L);
        ap.setStatus(ApprovalStatus.approved);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        assertThatThrownBy(() -> service.approve(7L, new ApprovalOpinionRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void reject_退回销毁清册回草稿() {
        ApprovalRequest ap = pending(ApprovalType.destruction, "destruction_list", 1L);
        when(approvalMapper.selectById(7L)).thenReturn(ap);
        DestructionList list = new DestructionList();
        list.setId(1L);
        list.setStatus(DestructionListStatus.pending_approval);
        list.setListNo("DES-000001");
        when(destructionListMapper.selectById(1L)).thenReturn(list);

        ApprovalOpinionRequest req = new ApprovalOpinionRequest();
        req.setOpinion("依据不足");
        service.reject(7L, req);

        ArgumentCaptor<ApprovalRequest> apCap = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvalMapper).updateById(apCap.capture());
        assertThat(apCap.getValue().getStatus()).isEqualTo(ApprovalStatus.rejected);
        assertThat(apCap.getValue().getApprovalOpinion()).isEqualTo("依据不足");
        ArgumentCaptor<DestructionList> listCap = ArgumentCaptor.forClass(DestructionList.class);
        verify(destructionListMapper).updateById(listCap.capture());
        assertThat(listCap.getValue().getStatus()).isEqualTo(DestructionListStatus.draft);
    }

    @Test
    void reject_退回意见为空抛校验失败() {
        ApprovalRequest ap = pending(ApprovalType.security_adjust, "archive", 10L);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        assertThatThrownBy(() -> service.reject(7L, new ApprovalOpinionRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    private ApprovalRequest pending(ApprovalType type, String targetType, Long targetId) {
        ApprovalRequest ap = new ApprovalRequest();
        ap.setId(7L);
        ap.setApprovalType(type);
        ap.setTargetType(targetType);
        ap.setTargetId(targetId);
        ap.setStatus(ApprovalStatus.pending);
        return ap;
    }
}
