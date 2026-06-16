package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.dto.request.InventoryTaskCreateRequest;
import com.archive.dto.request.InventoryTaskQuery;
import com.archive.dto.response.InventoryTaskDetailResponse;
import com.archive.entity.InventoryItem;
import com.archive.entity.InventoryTask;
import com.archive.enums.InventoryCheckResult;
import com.archive.enums.InventoryTaskStatus;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.InventoryItemMapper;
import com.archive.mapper.InventoryTaskMapper;
import com.archive.util.InventoryTaskNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final long ROOM_ID = 10L;
    private static final int CATEGORY_ID = 3;

    private InventoryService service;
    private InventoryTaskMapper taskMapper;
    private InventoryItemMapper itemMapper;
    private JdbcTemplate jdbc;
    private InventoryTaskNoUtil noUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        taskMapper = mock(InventoryTaskMapper.class);
        itemMapper = mock(InventoryItemMapper.class);
        jdbc = mock(JdbcTemplate.class);
        noUtil = mock(InventoryTaskNoUtil.class);
        auditService = mock(AuditService.class);
        service = new InventoryService(taskMapper, itemMapper, jdbc, noUtil, auditService);
    }

    private void asBack(Runnable body) {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(true);
            body.run();
        }
    }

    @Test
    void create_生成明细且借出档案标记on_loan() {
        when(noUtil.generate()).thenReturn("INV-000001");
        when(taskMapper.insert(any(InventoryTask.class))).thenAnswer(inv -> {
            ((InventoryTask) inv.getArgument(0)).setId(1L);
            return 1;
        });
        // 一条已借出(loan_status=on_loan)、一条正常(available)
        when(jdbc.queryForList(anyString(), eq(ROOM_ID), eq(CATEGORY_ID))).thenReturn(List.of(
                Map.of("archive_id", 100, "box_id", 5, "location_id", 7, "loan_status", "on_loan"),
                Map.of("archive_id", 101, "box_id", 5, "location_id", 7, "loan_status", "available")));
        when(taskMapper.selectById(1L)).thenReturn(draftTask(1L));
        when(itemMapper.selectList(any())).thenReturn(List.of());
        // getDetail 内 loadRoomNos/loadCategoryNames/loadTaskStats 用单参 queryForList 回填展示字段
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        InventoryTaskCreateRequest req = new InventoryTaskCreateRequest();
        req.setTaskName("401 盘点");
        req.setRoomId(ROOM_ID);
        req.setCategoryId(CATEGORY_ID);

        asBack(() -> {
            InventoryTaskDetailResponse resp = service.create(req);
            assertThat(resp.getTaskNo()).isEqualTo("INV-000001");
        });

        ArgumentCaptor<InventoryItem> cap = ArgumentCaptor.forClass(InventoryItem.class);
        verify(itemMapper, times(2)).insert(cap.capture());
        assertThat(cap.getAllValues().get(0).getCheckResult()).isEqualTo(InventoryCheckResult.on_loan);
        assertThat(cap.getAllValues().get(1).getCheckResult()).isEqualTo(InventoryCheckResult.normal);
    }

    @Test
    void start_空明细抛冲突() {
        when(taskMapper.selectById(1L)).thenReturn(draftTask(1L));
        when(itemMapper.selectCount(any())).thenReturn(0L);
        asBack(() -> assertThatThrownBy(() -> service.start(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("明细"));
    }

    @Test
    void 非后台角色抛FORBIDDEN() {
        try (MockedStatic<AuthContext> a = mockStatic(AuthContext.class)) {
            a.when(() -> AuthContext.hasRole(RoleCode.back_archivist)).thenReturn(false);
            assertThatThrownBy(() -> service.list(new InventoryTaskQuery()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无操作权限");
        }
    }

    private InventoryTask draftTask(Long id) {
        InventoryTask t = new InventoryTask();
        t.setId(id);
        t.setTaskNo("INV-000001");
        t.setStatus(InventoryTaskStatus.draft);
        t.setRoomId(ROOM_ID);
        t.setCategoryId(CATEGORY_ID);
        return t;
    }
}
