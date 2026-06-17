package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.AnalysisItemHandleRequest;
import com.archive.dto.request.AnalysisRule;
import com.archive.dto.request.AnalysisTaskCreateRequest;
import com.archive.dto.response.AnalysisTaskResponse;
import com.archive.entity.AnalysisItem;
import com.archive.entity.AnalysisTask;
import com.archive.entity.Archive;
import com.archive.enums.AnalysisIssueType;
import com.archive.enums.AnalysisItemStatus;
import com.archive.enums.AnalysisScanMethod;
import com.archive.enums.CarrierStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.AnalysisItemMapper;
import com.archive.mapper.AnalysisTaskMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.util.AnalysisTaskNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private AnalysisService service;
    private AnalysisTaskMapper taskMapper;
    private AnalysisItemMapper itemMapper;
    private ArchiveMapper archiveMapper;
    private JdbcTemplate jdbcTemplate;
    private AnalysisTaskNoUtil noUtil;
    private AiTaskService aiTaskService;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        taskMapper = mock(AnalysisTaskMapper.class);
        itemMapper = mock(AnalysisItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        noUtil = mock(AnalysisTaskNoUtil.class);
        aiTaskService = mock(AiTaskService.class);
        auditService = mock(AuditService.class);
        service = new AnalysisService(taskMapper, itemMapper, archiveMapper, jdbcTemplate, noUtil, aiTaskService, auditService);
    }

    private void asBack(Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(true);
            a.when(AuthContext::getCurrentUserId).thenReturn(50L);
            body.run();
        }
    }

    @Test
    void 规则扫描_缺字段产出missing_field项() {
        when(noUtil.generate()).thenReturn("ANA-000001");
        when(taskMapper.insert(any(AnalysisTask.class))).thenAnswer(inv -> {
            ((AnalysisTask) inv.getArgument(0)).setId(1L);
            return 1;
        });
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenReturn(List.of(1L));
        when(archiveMapper.selectById(1L)).thenReturn(archiveNullTitle());

        AnalysisTaskCreateRequest req = new AnalysisTaskCreateRequest();
        req.setScanMethod(AnalysisScanMethod.rule);
        AnalysisRule rule = new AnalysisRule();
        rule.setCategoryIds(List.of(3));
        rule.setIncludeAiSuggestion(false);
        req.setRule(rule);

        asBack(() -> service.create(req));

        ArgumentCaptor<AnalysisItem> cap = ArgumentCaptor.forClass(AnalysisItem.class);
        verify(itemMapper).insert(cap.capture());
        assertThat(cap.getValue().getIssueType()).isEqualTo(AnalysisIssueType.missing_field);
    }

    @Test
    void AI不可用_create直接completed且不产异常项() {
        when(noUtil.generate()).thenReturn("ANA-000001");
        when(taskMapper.insert(any(AnalysisTask.class))).thenAnswer(inv -> {
            ((AnalysisTask) inv.getArgument(0)).setId(2L);
            return 1;
        });
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenReturn(List.of(1L));
        when(archiveMapper.selectById(1L)).thenReturn(archiveComplete());
        when(aiTaskService.aiAvailable()).thenReturn(false);

        AnalysisTaskCreateRequest req = new AnalysisTaskCreateRequest();
        req.setScanMethod(AnalysisScanMethod.mixed);
        AnalysisRule rule = new AnalysisRule();
        rule.setCategoryIds(List.of(3));
        rule.setIncludeAiSuggestion(true);
        req.setRule(rule);

        asBack(() -> {
            AnalysisTaskResponse resp = service.create(req);
            assertThat(resp.getStatus()).isEqualTo("completed");
        });
        verify(itemMapper, never()).insert(any(AnalysisItem.class));
    }

    @Test
    void handle_仅改状态不改档案() {
        AnalysisItem item = new AnalysisItem();
        item.setId(10L);
        item.setStatus(AnalysisItemStatus.pending);
        when(itemMapper.selectById(10L)).thenReturn(item);

        AnalysisItemHandleRequest req = new AnalysisItemHandleRequest();
        req.setAction("adopted");
        req.setNote("采纳");

        asBack(() -> {
            var resp = service.handleItem(10L, req);
            assertThat(resp.getStatus()).isEqualTo("adopted");
        });
        verify(itemMapper).updateById(any(AnalysisItem.class));
        verify(archiveMapper, never()).selectById(any());
    }

    @Test
    void action非法抛BAD_REQUEST() {
        AnalysisItemHandleRequest req = new AnalysisItemHandleRequest();
        req.setAction("xyz");
        asBack(() -> assertThatThrownBy(() -> service.handleItem(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("adopted"));
    }

    private Archive archiveNullTitle() {
        Archive a = new Archive();
        a.setId(1L);
        a.setTitle(null);
        a.setResponsibleText(null);
        a.setFormedDate(null);
        return a;
    }

    private Archive archiveComplete() {
        Archive a = new Archive();
        a.setId(1L);
        a.setTitle("完整题名");
        a.setResponsibleText("责任者");
        a.setFormedDate(LocalDate.of(2024, 1, 1));
        a.setCarrierStatus(CarrierStatus.electronic);
        a.setSecurityLevel(0);
        a.setOpenStatus("open");
        return a;
    }
}
