package com.archive.service;

import com.archive.config.FileProperties;
import com.archive.dto.request.DestructionDestroyRequest;
import com.archive.dto.request.DestructionSubmitRequest;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveBox;
import com.archive.entity.ArchiveBoxItem;
import com.archive.entity.ArchiveFile;
import com.archive.entity.ApprovalRequest;
import com.archive.entity.BusinessAttachment;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.ApprovalStatus;
import com.archive.enums.DestroyMethod;
import com.archive.enums.DestructionListStatus;
import com.archive.enums.FileStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.ScanResult;
import com.archive.exception.BusinessException;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BusinessAttachmentMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DestructionServiceTest {

    private DestructionService service;
    private DestructionListMapper listMapper;
    private DestructionItemMapper itemMapper;
    private ArchiveMapper archiveMapper;
    private ApprovalRequestMapper approvalMapper;
    private BusinessAttachmentMapper attachmentMapper;
    private ArchiveFileMapper archiveFileMapper;
    private ArchiveBoxItemMapper archiveBoxItemMapper;
    private ArchiveBoxMapper archiveBoxMapper;
    private AppraisalBatchMapper appraisalBatchMapper;
    private UserMapper userMapper;
    private MinioService minioService;
    private ClamAvScanner clamAvScanner;
    private FileProperties fileProperties;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        listMapper = mock(DestructionListMapper.class);
        itemMapper = mock(DestructionItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        approvalMapper = mock(ApprovalRequestMapper.class);
        attachmentMapper = mock(BusinessAttachmentMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        archiveBoxItemMapper = mock(ArchiveBoxItemMapper.class);
        archiveBoxMapper = mock(ArchiveBoxMapper.class);
        appraisalBatchMapper = mock(AppraisalBatchMapper.class);
        userMapper = mock(UserMapper.class);
        minioService = mock(MinioService.class);
        clamAvScanner = mock(ClamAvScanner.class);
        fileProperties = mock(FileProperties.class);
        auditService = mock(AuditService.class);

        service = new DestructionService(listMapper, itemMapper, archiveMapper, approvalMapper,
                attachmentMapper, archiveFileMapper, archiveBoxItemMapper, archiveBoxMapper,
                appraisalBatchMapper, userMapper,
                minioService, clamAvScanner, fileProperties, auditService);
    }

    @Test
    void listLists_分页并回填明细数() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setListNo("DES-000001");
        l.setStatus(DestructionListStatus.draft);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DestructionList> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(l));
        page.setTotal(1L);
        when(listMapper.selectPage(any(), any())).thenReturn(page);
        when(itemMapper.selectCount(any())).thenReturn(4L);

        var r = service.listLists(null, null, 1, 20);

        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getRecords().get(0).getItemCount()).isEqualTo(4);
    }

    @Test
    void getListDetail_聚合明细审批与照片() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.pending_destroy);
        l.setApprovalRequestId(7L);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionItem it = new DestructionItem();
        it.setArchiveId(10L);
        it.setArchiveNoSnapshot("ARC-000010");
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        ApprovalRequest ap = new ApprovalRequest();
        ap.setStatus(ApprovalStatus.approved);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        BusinessAttachment photo = new BusinessAttachment();
        photo.setId(20L);
        photo.setOriginalFilename("p1.jpg");
        photo.setMimeType("image/jpeg");
        photo.setFileSize(1024L);
        when(attachmentMapper.selectList(any())).thenReturn(List.of(photo));

        var resp = service.getListDetail(1L);

        assertThat(resp.getApprovalStatus()).isEqualTo("approved");
        assertThat(resp.getPhotos()).hasSize(1);
        assertThat(resp.getPhotos().get(0).getOriginalFilename()).isEqualTo("p1.jpg");
    }

    @Test
    void submitApproval_创建销毁审批单并置待审批() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.draft);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionItem it = new DestructionItem();
        it.setArchiveId(10L);
        when(itemMapper.selectList(any())).thenReturn(List.of(it));
        Archive a = new Archive();
        a.setId(10L);
        a.setLifecycleStatus(LifecycleStatus.pending_destruction);
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a));

        DestructionSubmitRequest req = new DestructionSubmitRequest();
        req.setReason("到期销毁");

        // 模拟 MyBatis-Plus insert 回填自增 id
        when(approvalMapper.insert(any(ApprovalRequest.class))).thenAnswer(inv -> {
            ((ApprovalRequest) inv.getArgument(0)).setId(99L);
            return 1;
        });

        service.submitApproval(1L, req);

        ArgumentCaptor<ApprovalRequest> cap = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvalMapper).insert(cap.capture());
        assertThat(cap.getValue().getApprovalType()).isEqualTo(com.archive.enums.ApprovalType.destruction);
        assertThat(cap.getValue().getTargetId()).isEqualTo(1L);
        assertThat(cap.getValue().getStatus()).isEqualTo(ApprovalStatus.pending);
        verify(listMapper).updateById(any(DestructionList.class));
    }

    @Test
    void submitApproval_非草稿清册抛冲突() {
        DestructionList l = new DestructionList();
        l.setStatus(DestructionListStatus.pending_approval);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionSubmitRequest req = new DestructionSubmitRequest();
        req.setReason("x");
        assertThatThrownBy(() -> service.submitApproval(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void confirmDestroy_非待销毁清册抛冲突() {
        DestructionList l = new DestructionList();
        l.setStatus(DestructionListStatus.pending_approval);
        when(listMapper.selectById(1L)).thenReturn(l);

        DestructionDestroyRequest req = new DestructionDestroyRequest();
        req.setDestroyMethod("shredding");
        req.setSupervisorName1("甲");
        req.setSupervisorName2("乙");
        assertThatThrownBy(() -> service.confirmDestroy(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void confirmDestroy_置销毁并解除盒关系标记电子文件删除() {
        DestructionList l = new DestructionList();
        l.setId(1L);
        l.setStatus(DestructionListStatus.pending_destroy);
        l.setApprovalRequestId(7L);
        when(listMapper.selectById(1L)).thenReturn(l);

        ApprovalRequest ap = new ApprovalRequest();
        ap.setStatus(ApprovalStatus.approved);
        when(approvalMapper.selectById(7L)).thenReturn(ap);

        DestructionItem it = new DestructionItem();
        it.setId(50L);
        it.setArchiveId(10L);
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        Archive a = new Archive();
        a.setId(10L);
        a.setLifecycleStatus(LifecycleStatus.pending_destruction);
        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(a));

        ArchiveFile f = new ArchiveFile();
        f.setId(80L);
        f.setArchiveId(10L);
        when(archiveFileMapper.selectList(any())).thenReturn(List.of(f));

        ArchiveBoxItem bi = new ArchiveBoxItem();
        bi.setId(90L);
        bi.setBoxId(5L);
        bi.setArchiveId(10L);
        when(archiveBoxItemMapper.selectList(any())).thenReturn(List.of(bi));
        ArchiveBox box = new ArchiveBox();
        box.setId(5L);
        box.setUsedCount(3);
        when(archiveBoxMapper.selectById(5L)).thenReturn(box);

        DestructionDestroyRequest req = new DestructionDestroyRequest();
        req.setDestroyMethod("shredding");
        req.setSupervisorName1("甲");
        req.setSupervisorName2("乙");
        req.setDestroyNote("现场粉碎");

        service.confirmDestroy(1L, req);

        // 档案置 destroyed
        ArgumentCaptor<Archive> archiveCap = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).updateById(archiveCap.capture());
        assertThat(archiveCap.getValue().getLifecycleStatus()).isEqualTo(LifecycleStatus.destroyed);
        assertThat(archiveCap.getValue().getConditionStatus()).isEqualTo(com.archive.enums.ConditionStatus.destroyed);
        // 电子文件标记删除
        ArgumentCaptor<ArchiveFile> fileCap = ArgumentCaptor.forClass(ArchiveFile.class);
        verify(archiveFileMapper).updateById(fileCap.capture());
        assertThat(fileCap.getValue().getFileStatus()).isEqualTo(FileStatus.deleted);
        // 盒关系解除 + 盒内数量递减
        verify(archiveBoxItemMapper).delete(any());
        ArgumentCaptor<ArchiveBox> boxCap = ArgumentCaptor.forClass(ArchiveBox.class);
        verify(archiveBoxMapper).updateById(boxCap.capture());
        assertThat(boxCap.getValue().getUsedCount()).isEqualTo(2);
        // 清册置 destroyed
        ArgumentCaptor<DestructionList> listCap = ArgumentCaptor.forClass(DestructionList.class);
        verify(listMapper).updateById(listCap.capture());
        assertThat(listCap.getValue().getStatus()).isEqualTo(DestructionListStatus.destroyed);
        assertThat(listCap.getValue().getDestroyMethod()).isEqualTo(DestroyMethod.shredding);
        verify(auditService).log(eq("M11"), eq("destroy"), eq("destruction_list"), eq(1L), any());
    }

    @Test
    void uploadPhotos_清册不存在抛404() {
        when(listMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.uploadPhotos(99L, new MultipartFile[0]))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(com.archive.common.ErrorCode.NOT_FOUND);
    }

    @Test
    void uploadPhotos_扫描通过后创建destruction_photo附件() throws Exception {
        DestructionList l = new DestructionList();
        l.setId(1L);
        when(listMapper.selectById(1L)).thenReturn(l);
        when(fileProperties.getBucket()).thenReturn("archive");
        when(fileProperties.getAllowedExtensions()).thenReturn(java.util.List.of("jpg", "png"));
        when(fileProperties.getMaxSize()).thenReturn(10L * 1024 * 1024);
        FileProperties.Scan scan = mock(FileProperties.Scan.class);
        when(scan.isEnabled()).thenReturn(true);
        when(fileProperties.getScan()).thenReturn(scan);
        when(clamAvScanner.scan(any())).thenReturn(ScanResult.safe);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("scene1.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(2048L);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(file.isEmpty()).thenReturn(false);

        var result = service.uploadPhotos(1L, new MultipartFile[]{file});

        ArgumentCaptor<BusinessAttachment> attCap = ArgumentCaptor.forClass(BusinessAttachment.class);
        verify(attachmentMapper).insert(attCap.capture());
        assertThat(attCap.getValue().getBusinessType()).isEqualTo("destruction_list");
        assertThat(attCap.getValue().getAttachmentType()).isEqualTo("destruction_photo");
        assertThat(attCap.getValue().getBusinessId()).isEqualTo(1L);
        assertThat(result).hasSize(1);
    }
}
