package com.archive.service;

import com.archive.dto.response.AdminDashboardResponse;
import com.archive.dto.response.WarehouseWarningResponse;
import com.archive.entity.AuditLog;
import com.archive.entity.User;
import com.archive.mapper.AppraisalBatchMapper;
import com.archive.mapper.ApprovalRequestMapper;
import com.archive.mapper.ArchiveFileMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.AuditLogMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.archive.mapper.DestructionListMapper;
import com.archive.mapper.IntakeBatchMapper;
import com.archive.mapper.SystemConfigMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {

    private AdminDashboardService service;
    private IntakeBatchMapper intakeBatchMapper;
    private BorrowRequestMapper borrowRequestMapper;
    private ApprovalRequestMapper approvalRequestMapper;
    private DestructionListMapper destructionListMapper;
    private ArchiveMapper archiveMapper;
    private AppraisalBatchMapper appraisalBatchMapper;
    private ArchiveFileMapper archiveFileMapper;
    private AuditLogMapper auditLogMapper;
    private UserMapper userMapper;
    private SystemConfigMapper systemConfigMapper;
    private WarehouseService warehouseService;

    @BeforeEach
    void setup() {
        intakeBatchMapper = mock(IntakeBatchMapper.class);
        borrowRequestMapper = mock(BorrowRequestMapper.class);
        approvalRequestMapper = mock(ApprovalRequestMapper.class);
        destructionListMapper = mock(DestructionListMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        appraisalBatchMapper = mock(AppraisalBatchMapper.class);
        archiveFileMapper = mock(ArchiveFileMapper.class);
        auditLogMapper = mock(AuditLogMapper.class);
        userMapper = mock(UserMapper.class);
        systemConfigMapper = mock(SystemConfigMapper.class);
        warehouseService = mock(WarehouseService.class);
        service = new AdminDashboardService(intakeBatchMapper, borrowRequestMapper, approvalRequestMapper,
                destructionListMapper, archiveMapper, appraisalBatchMapper, archiveFileMapper,
                auditLogMapper, userMapper, systemConfigMapper, warehouseService);
    }

    @Test
    void overview_聚合计数与存储用量() {
        when(intakeBatchMapper.selectCount(any())).thenReturn(3L, 7L); // 待验收=3, 待入库=7
        when(borrowRequestMapper.selectCount(any())).thenReturn(6L);
        when(approvalRequestMapper.selectCount(any())).thenReturn(9L);
        when(destructionListMapper.selectCount(any())).thenReturn(2L);
        // archiveMapper 三次：pendingShelf=5, totalArchives=1000, monthAdded=50
        when(archiveMapper.selectCount(any())).thenReturn(5L, 1000L, 50L);
        when(appraisalBatchMapper.selectCount(any())).thenReturn(64L);
        when(archiveFileMapper.sumFileSize()).thenReturn(50L * 1024 * 1024 * 1024); // 50GB
        when(systemConfigMapper.selectOne(any())).thenReturn(null); // 用默认配额500GB、阈值0.85

        AdminDashboardResponse resp = service.overview();

        assertThat(resp.getTodos().getPendingTransferReception()).isEqualTo(3L);
        assertThat(resp.getTodos().getPendingArchive()).isEqualTo(7L);
        assertThat(resp.getTodos().getBorrowApproval()).isEqualTo(6L);
        assertThat(resp.getTodos().getApprovalPending()).isEqualTo(9L);
        assertThat(resp.getTodos().getPendingDestruction()).isEqualTo(2L);
        assertThat(resp.getTodos().getPendingShelf()).isEqualTo(5L);
        assertThat(resp.getTodos().getAppraisalDue()).isEqualTo(64L);

        assertThat(resp.getArchiveSummary().getTotalArchives()).isEqualTo(1000L);
        assertThat(resp.getArchiveSummary().getMonthAdded()).isEqualTo(50L);
        // 50GB / 500GB = 0.1
        assertThat(resp.getArchiveSummary().getStorageUsage()).isCloseTo(0.1, within(0.001));
        assertThat(resp.getArchiveSummary().getStorageWarningThreshold()).isCloseTo(0.85, within(0.001));
        assertThat(resp.getSummarizedAt()).isNotNull();
    }

    @Test
    void warehouseWarnings_映射库房告警() {
        zeroCounts();
        WarehouseWarningResponse room = new WarehouseWarningResponse();
        room.setRoomNo("402");
        room.setRoomName("会计档案库房");
        room.setOccupancyRate(new BigDecimal("0.88"));
        room.setWarningThreshold(new BigDecimal("0.85"));
        when(warehouseService.listWarningRooms()).thenReturn(List.of(room));

        AdminDashboardResponse resp = service.overview();

        assertThat(resp.getWarehouseWarnings()).hasSize(1);
        assertThat(resp.getWarehouseWarnings().get(0).getRoomNo()).isEqualTo("402");
        assertThat(resp.getWarehouseWarnings().get(0).getOccupancyRate()).isCloseTo(0.88, within(0.001));
    }

    @Test
    void recentAuditLogs_操作人名解析() {
        zeroCounts();
        AuditLog systemLog = log(1L, "system", null, "M01", "auto_backup");
        AuditLog userLog = log(2L, "internal", 10L, "M06", "create_organization");
        when(auditLogMapper.selectList(any())).thenReturn(List.of(userLog, systemLog));
        User u = new User();
        u.setId(10L);
        u.setRealName("小陈");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(u));

        AdminDashboardResponse resp = service.overview();

        assertThat(resp.getRecentAuditLogs()).hasSize(2);
        // userLog 在前 → 操作人解析为 realName
        assertThat(resp.getRecentAuditLogs().get(0).getOperator()).isEqualTo("小陈");
        assertThat(resp.getRecentAuditLogs().get(0).getAction()).isEqualTo("create_organization");
        // systemLog → "系统"
        assertThat(resp.getRecentAuditLogs().get(1).getOperator()).isEqualTo("系统");
    }

    /** 把所有计数置 0，聚焦非计数字段的验证。 */
    private void zeroCounts() {
        when(intakeBatchMapper.selectCount(any())).thenReturn(0L);
        when(borrowRequestMapper.selectCount(any())).thenReturn(0L);
        when(approvalRequestMapper.selectCount(any())).thenReturn(0L);
        when(destructionListMapper.selectCount(any())).thenReturn(0L);
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(appraisalBatchMapper.selectCount(any())).thenReturn(0L);
        when(archiveFileMapper.sumFileSize()).thenReturn(0L);
        when(systemConfigMapper.selectOne(any())).thenReturn(null);
        when(auditLogMapper.selectList(any())).thenReturn(List.of());
        when(warehouseService.listWarningRooms()).thenReturn(List.of());
    }

    private AuditLog log(long id, String actorType, Long actorUserId, String module, String op) {
        AuditLog l = new AuditLog();
        l.setId(id);
        l.setActorType(actorType);
        l.setActorUserId(actorUserId);
        l.setModuleName(module);
        l.setOperationType(op);
        l.setOperatedAt(OffsetDateTime.now());
        return l;
    }

    private static org.assertj.core.data.Offset<Double> within(double tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }
}
