package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.AppraisalBatchCreateRequest;
import com.archive.dto.request.AppraisalItemSaveRequest;
import com.archive.entity.AppraisalBatch;
import com.archive.entity.AppraisalItem;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveChangeLog;
import com.archive.entity.DestructionItem;
import com.archive.entity.DestructionList;
import com.archive.enums.AppraisalBatchStatus;
import com.archive.enums.AppraisalResult;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.RetentionPeriod;
import com.archive.exception.BusinessException;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.AppraisalItemMapper;
import com.archive.mapper.ArchiveChangeLogMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.DestructionItemMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.util.AppraisalNoUtil;
import com.archive.util.DestructionNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AppraisalServiceTest {

    private AppraisalService service;
    private AppraisalBatchMapper batchMapper;
    private AppraisalItemMapper itemMapper;
    private ArchiveMapper archiveMapper;
    private DestructionListMapper destructionListMapper;
    private DestructionItemMapper destructionItemMapper;
    private ArchiveChangeLogMapper changeLogMapper;
    private AppraisalNoUtil appraisalNoUtil;
    private DestructionNoUtil destructionNoUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        batchMapper = mock(AppraisalBatchMapper.class);
        itemMapper = mock(AppraisalItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        destructionListMapper = mock(DestructionListMapper.class);
        destructionItemMapper = mock(DestructionItemMapper.class);
        changeLogMapper = mock(ArchiveChangeLogMapper.class);
        appraisalNoUtil = mock(AppraisalNoUtil.class);
        destructionNoUtil = mock(DestructionNoUtil.class);
        auditService = mock(AuditService.class);

        service = new AppraisalService(batchMapper, itemMapper, archiveMapper,
                destructionListMapper, destructionItemMapper, changeLogMapper,
                appraisalNoUtil, destructionNoUtil, auditService);
    }

    @Test
    void createBatch_年度范围反了抛校验失败() {
        AppraisalBatchCreateRequest req = new AppraisalBatchCreateRequest();
        req.setBatchName("批次");
        req.setFormedYearStart(2020);
        req.setFormedYearEnd(2019);

        assertThatThrownBy(() -> service.createBatch(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void createBatch_生成批次号并命中到期档案写入明细() {
        AppraisalBatchCreateRequest req = new AppraisalBatchCreateRequest();
        req.setBatchName("2026 到期会计鉴定");
        req.setCategoryId(3);
        req.setFormedYearStart(2014);
        req.setFormedYearEnd(2014);
        when(appraisalNoUtil.generate()).thenReturn("APP-000001");

        Archive a1 = archive(10L, "ARC-000010", "凭证A", 2014, RetentionPeriod._10y, LocalDate.of(2024, 12, 31));
        Archive a2 = archive(11L, "ARC-000011", "凭证B", 2014, RetentionPeriod._10y, LocalDate.of(2024, 6, 1));
        when(archiveMapper.selectList(any())).thenReturn(List.of(a1, a2));

        ArgumentCaptor<AppraisalItem> captor = ArgumentCaptor.forClass(AppraisalItem.class);

        var resp = service.createBatch(req);

        assertThat(resp.getBatchNo()).isEqualTo("APP-000001");
        assertThat(resp.getStatus()).isEqualTo("draft");
        assertThat(resp.getItems()).hasSize(2);
        verify(batchMapper).insert(any(AppraisalBatch.class));
        verify(itemMapper, times(2)).insert(captor.capture());
        assertThat(captor.getValue().getArchiveId()).isIn(10L, 11L);
        assertThat(captor.getValue().getAppraisalResult()).isNull();
        verify(auditService).log(eq("M10"), eq("create_appraisal_batch"), eq("appraisal_batch"), any(), any());
    }

    @Test
    void listBatches_分页并回填明细数() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setBatchNo("APP-000001");
        b.setStatus(AppraisalBatchStatus.draft);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AppraisalBatch> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(b));
        page.setTotal(1L);
        when(batchMapper.selectPage(any(), any())).thenReturn(page);
        when(itemMapper.selectCount(any())).thenReturn(5L);

        var r = service.listBatches(null, null, null, null, 1, 20);

        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getRecords().get(0).getItemCount()).isEqualTo(5);
        assertThat(r.getRecords().get(0).getStatus()).isEqualTo("draft");
    }

    @Test
    void getBatchDetail_回填档案信息与鉴定结论() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem it = new AppraisalItem();
        it.setArchiveId(10L);
        it.setAppraisalResult(AppraisalResult.destroy);
        it.setOpinion("到期");
        when(itemMapper.selectList(any())).thenReturn(List.of(it));
        when(archiveMapper.selectBatchIds(any()))
                .thenReturn(List.of(archive(10L, "ARC-000010", "凭证A", 2014, RetentionPeriod._10y, LocalDate.of(2024, 12, 31))));

        var resp = service.getBatchDetail(1L);

        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getArchiveNo()).isEqualTo("ARC-000010");
        assertThat(resp.getItems().get(0).getAppraisalResult()).isEqualTo("destroy");
    }

    @Test
    void getBatchDetail_批次不存在抛404() {
        when(batchMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getBatchDetail(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void saveItems_非草稿批次抛冲突() {
        AppraisalBatch b = new AppraisalBatch();
        b.setStatus(AppraisalBatchStatus.completed);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItemSaveRequest req = new AppraisalItemSaveRequest();
        req.setItems(List.of());
        assertThatThrownBy(() -> service.saveItems(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void saveItems_延长未填保管期限抛校验失败() {
        AppraisalBatch b = new AppraisalBatch();
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItemSaveRequest req = new AppraisalItemSaveRequest();
        AppraisalItemSaveRequest.Item it = new AppraisalItemSaveRequest.Item();
        it.setArchiveId(10L);
        it.setAppraisalResult("extend");
        req.setItems(List.of(it));

        assertThatThrownBy(() -> service.saveItems(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void saveItems_更新明细结论() {
        AppraisalBatch b = new AppraisalBatch();
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem existing = new AppraisalItem();
        existing.setArchiveId(10L);
        when(itemMapper.selectOne(any())).thenReturn(existing);
        when(itemMapper.selectList(any())).thenReturn(List.of(existing));
        when(archiveMapper.selectBatchIds(any()))
                .thenReturn(List.of(archive(10L, "ARC-000010", "凭证A", 2014, RetentionPeriod._10y, LocalDate.of(2024, 12, 31))));

        AppraisalItemSaveRequest req = new AppraisalItemSaveRequest();
        AppraisalItemSaveRequest.Item it = new AppraisalItemSaveRequest.Item();
        it.setArchiveId(10L);
        it.setAppraisalResult("destroy");
        it.setOpinion("到期销毁");
        req.setItems(List.of(it));

        service.saveItems(1L, req);

        ArgumentCaptor<AppraisalItem> itemCaptor = ArgumentCaptor.forClass(AppraisalItem.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getAppraisalResult()).isEqualTo(AppraisalResult.destroy);
    }

    @Test
    void completeBatch_存在未填结论的明细抛校验失败() {
        AppraisalBatch b = new AppraisalBatch();
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem it = new AppraisalItem();
        it.setArchiveId(10L);
        it.setAppraisalResult(null);
        when(itemMapper.selectList(any())).thenReturn(List.of(it));

        assertThatThrownBy(() -> service.completeBatch(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void completeBatch_extend更新期限写日志且destroy生成销毁清册快照() {
        AppraisalBatch b = new AppraisalBatch();
        b.setId(1L);
        b.setBatchName("2026 到期会计");
        b.setStatus(AppraisalBatchStatus.draft);
        when(batchMapper.selectById(1L)).thenReturn(b);

        AppraisalItem ext = new AppraisalItem();
        ext.setArchiveId(10L);
        ext.setAppraisalResult(AppraisalResult.extend);
        ext.setNewRetentionPeriod("30y");
        ext.setOpinion("延长保管");
        AppraisalItem des = new AppraisalItem();
        des.setArchiveId(11L);
        des.setAppraisalResult(AppraisalResult.destroy);
        des.setOpinion("到期销毁");
        when(itemMapper.selectList(any())).thenReturn(List.of(ext, des));

        when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(
                archive(10L, "ARC-000010", "凭证A", 2014, RetentionPeriod._10y, LocalDate.of(2024, 12, 31)),
                archive(11L, "ARC-000011", "凭证B", 2014, RetentionPeriod._10y, LocalDate.of(2024, 6, 1))));
        when(destructionNoUtil.generate()).thenReturn("DES-000001");

        service.completeBatch(1L);

        ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper, times(2)).updateById(archiveCaptor.capture());
        java.util.List<Archive> updated = archiveCaptor.getAllValues();
        Archive a10 = updated.stream().filter(a -> a.getId() == 10L).findFirst().orElseThrow();
        assertThat(a10.getRetentionPeriod()).isEqualTo(RetentionPeriod._30y);
        Archive a11 = updated.stream().filter(a -> a.getId() == 11L).findFirst().orElseThrow();
        assertThat(a11.getLifecycleStatus()).isEqualTo(LifecycleStatus.pending_destruction);

        ArgumentCaptor<ArchiveChangeLog> logCaptor = ArgumentCaptor.forClass(ArchiveChangeLog.class);
        verify(changeLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getFieldName()).isEqualTo("retention_period");

        ArgumentCaptor<DestructionItem> diCaptor = ArgumentCaptor.forClass(DestructionItem.class);
        verify(destructionItemMapper).insert(diCaptor.capture());
        assertThat(diCaptor.getValue().getArchiveId()).isEqualTo(11L);
        assertThat(diCaptor.getValue().getArchiveNoSnapshot()).isEqualTo("ARC-000011");
        assertThat(diCaptor.getValue().getAppraisalOpinionSnapshot()).isEqualTo("到期销毁");

        verify(destructionListMapper).insert(any(DestructionList.class));

        ArgumentCaptor<AppraisalBatch> batchCaptor = ArgumentCaptor.forClass(AppraisalBatch.class);
        verify(batchMapper).updateById(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo(AppraisalBatchStatus.completed);
        verify(auditService).log(eq("M10"), eq("complete_appraisal"), eq("appraisal_batch"), eq(1L), any());
    }

    private Archive archive(long id, String no, String title, int year, RetentionPeriod rp, LocalDate until) {
        Archive a = new Archive();
        a.setId(id);
        a.setArchiveNo(no);
        a.setTitle(title);
        a.setFormedYear(year);
        a.setRetentionPeriod(rp);
        a.setRetentionUntil(until);
        a.setLifecycleStatus(LifecycleStatus.normal);
        return a;
    }
}
