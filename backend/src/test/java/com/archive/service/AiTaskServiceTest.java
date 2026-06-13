package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.response.AiTaskStartResponse;
import com.archive.entity.AiTask;
import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.exception.BusinessException;
import com.archive.mapper.AiTaskBatchMapper;
import com.archive.mapper.AiTaskMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.IntakeItemMapper;
import com.archive.util.AiTaskNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiTaskServiceTest {

    private AiTaskService service;
    private AiTaskMapper aiTaskMapper;
    private AiTaskBatchMapper aiTaskBatchMapper;
    private IntakeItemMapper intakeItemMapper;
    private IntakeBatchMapper intakeBatchMapper;
    private AiClient aiClient;
    private AiTaskNoUtil aiTaskNoUtil;
    private AuditService auditService;
    private AiTaskAsyncRunner asyncRunner;

    @BeforeEach
    void setup() {
        aiTaskMapper = mock(AiTaskMapper.class);
        aiTaskBatchMapper = mock(AiTaskBatchMapper.class);
        intakeItemMapper = mock(IntakeItemMapper.class);
        intakeBatchMapper = mock(IntakeBatchMapper.class);
        aiClient = mock(AiClient.class);
        aiTaskNoUtil = mock(AiTaskNoUtil.class);
        auditService = mock(AuditService.class);
        asyncRunner = mock(AiTaskAsyncRunner.class);
        service = new AiTaskService(aiTaskMapper, aiTaskBatchMapper, intakeItemMapper,
                intakeBatchMapper, aiClient, aiTaskNoUtil, auditService, asyncRunner);
    }

    @Test
    void splitIntoBatches_整除拆分() {
        List<Long> ids = LongStream.rangeClosed(1, 100).boxed().toList();
        List<List<Long>> out = AiTaskService.splitIntoBatches(ids, 50);
        assertThat(out).hasSize(2);
        assertThat(out.get(0)).hasSize(50);
        assertThat(out.get(1)).hasSize(50);
    }

    @Test
    void splitIntoBatches_非整除末批取剩余() {
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();
        List<List<Long>> out = AiTaskService.splitIntoBatches(ids, 50);
        assertThat(out).hasSize(3);
        assertThat(out.get(2)).hasSize(1);
    }

    @Test
    void splitIntoBatches_空列表返回空() {
        assertThat(AiTaskService.splitIntoBatches(List.of(), 50)).isEmpty();
    }

    @Test
    void startIntakeCompletion_AI未启用抛外部异常() {
        when(aiClient.isAvailable()).thenReturn(false);
        assertThatThrownBy(() -> service.startIntakeCompletion(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
        verifyNoInteractions(asyncRunner);
    }

    @Test
    void startIntakeCompletion_无条目抛冲突() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(intakeBatchMapper.selectById(1L)).thenReturn(new IntakeBatch());
        when(intakeItemMapper.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.startIntakeCompletion(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void startIntakeCompletion_有运行中任务抛冲突() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(intakeBatchMapper.selectById(1L)).thenReturn(new IntakeBatch());
        List<IntakeItem> items = new ArrayList<>();
        IntakeItem it = new IntakeItem();
        it.setId(10L);
        items.add(it);
        when(intakeItemMapper.selectList(any())).thenReturn(items);
        when(aiTaskMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.startIntakeCompletion(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void startIntakeCompletion_正常创建任务批次并触发异步() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.getBatchSize()).thenReturn(50);
        when(aiTaskNoUtil.generate()).thenReturn("AIT-000001");
        when(intakeBatchMapper.selectById(1L)).thenReturn(new IntakeBatch());
        IntakeItem it = new IntakeItem();
        it.setId(10L);
        when(intakeItemMapper.selectList(any())).thenReturn(List.of(it));
        when(aiTaskMapper.selectCount(any())).thenReturn(0L);
        when(aiTaskMapper.insert(any(AiTask.class))).thenAnswer(inv -> {
            ((AiTask) inv.getArgument(0)).setId(99L);
            return 1;
        });

        AiTaskStartResponse resp = service.startIntakeCompletion(1L);

        assertThat(resp.getAiTaskId()).isEqualTo(99L);
        assertThat(resp.getStatus()).isEqualTo("running");
        assertThat(resp.getTotalBatches()).isEqualTo(1);
        verify(aiTaskBatchMapper, times(1)).insert(any(AiTaskBatch.class));
        verify(asyncRunner, times(1)).executeIntakeCompletion(99L);
    }

    @Test
    void retryFailed_非终态任务拒绝重试() {
        AiTask task = new AiTask();
        task.setTaskType("intake_completion");
        task.setStatus("running");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        assertThatThrownBy(() -> service.retryFailed(1L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode().getCode().equals(ErrorCode.BUSINESS_CONFLICT.getCode()));
    }

    @Test
    void retryFailed_无失败批次拒绝() {
        AiTask task = new AiTask();
        task.setStatus("partial_completed");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        when(aiTaskBatchMapper.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.retryFailed(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void retryFailed_重置失败批次并触发异步() {
        AiTask task = new AiTask();
        task.setId(1L);
        task.setTaskType("intake_completion");
        task.setStatus("partial_completed");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        AiTaskBatch failed = new AiTaskBatch();
        failed.setId(5L);
        failed.setStatus("failed");
        when(aiTaskBatchMapper.selectList(any())).thenReturn(List.of(failed));

        service.retryFailed(1L);

        assertThat(failed.getStatus()).isEqualTo("pending");
        assertThat(task.getStatus()).isEqualTo("running");
        verify(asyncRunner).executeIntakeCompletion(1L);
    }
}
