package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.LocationStatusRequest;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.entity.StorageLocation;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.StorageLocationMapper;
import com.archive.mapper.WarehouseRoomMapper;
import com.archive.util.BoxNoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WarehouseServiceTest {

    private WarehouseService service;
    private WarehouseRoomMapper warehouseRoomMapper;
    private StorageLocationMapper storageLocationMapper;
    private ArchiveBoxMapper archiveBoxMapper;
    private ArchiveBoxItemMapper archiveBoxItemMapper;
    private ArchiveMapper archiveMapper;
    private JdbcTemplate jdbcTemplate;
    private BoxNoUtil boxNoUtil;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        warehouseRoomMapper = mock(WarehouseRoomMapper.class);
        storageLocationMapper = mock(StorageLocationMapper.class);
        archiveBoxMapper = mock(ArchiveBoxMapper.class);
        archiveBoxItemMapper = mock(ArchiveBoxItemMapper.class);
        archiveMapper = mock(ArchiveMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        boxNoUtil = mock(BoxNoUtil.class);
        auditService = mock(AuditService.class);

        service = new WarehouseService(warehouseRoomMapper, storageLocationMapper,
                archiveBoxMapper, archiveBoxItemMapper, archiveMapper,
                jdbcTemplate, boxNoUtil, auditService);
    }

    @Test
    void createRoom_库房号重复抛冲突() {
        WarehouseRoomCreateRequest req = new WarehouseRoomCreateRequest();
        req.setRoomNo("401");
        req.setRoomName("A");
        req.setRackCount(1);
        req.setLayersPerRack(1);
        req.setBoxesPerLayer(1);
        when(warehouseRoomMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createRoom(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库房号已存在");
    }

    @Test
    void createRoom_生成正确数量与编码的架位() {
        WarehouseRoomCreateRequest req = new WarehouseRoomCreateRequest();
        req.setRoomNo("401");
        req.setRoomName("综合库房");
        req.setRackCount(2);
        req.setLayersPerRack(3);
        req.setBoxesPerLayer(4);
        when(warehouseRoomMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);

        WarehouseRoomResponse resp = service.createRoom(req);

        // 2*3*4 = 24 个架位
        verify(storageLocationMapper, times(24)).insert(captor.capture());
        List<StorageLocation> inserted = captor.getAllValues();
        assertThat(inserted).extracting(StorageLocation::getLocationCode)
                .contains("401-01-01-01", "401-02-03-04");
        assertThat(resp.getCapacity()).isEqualTo(24);
        assertThat(resp.getOccupancyRate()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(auditService).log(eq("M07"), eq("create_room"), eq("warehouse_room"), any(), any());
    }

    @Test
    void createRoom_阈值越界抛校验失败() {
        WarehouseRoomCreateRequest req = new WarehouseRoomCreateRequest();
        req.setRoomNo("402");
        req.setRoomName("B");
        req.setRackCount(1);
        req.setLayersPerRack(1);
        req.setBoxesPerLayer(1);
        req.setWarningThreshold(new BigDecimal("1.5"));
        when(warehouseRoomMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.createRoom(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void listRooms_计算占用率与告警状态() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        room.setRoomName("综合库房");
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        room.setStatus("active");
        when(warehouseRoomMapper.selectList(any())).thenReturn(List.of(room));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(90L);

        List<WarehouseRoomResponse> result = service.listRooms(null, null);

        assertThat(result).hasSize(1);
        WarehouseRoomResponse r = result.get(0);
        assertThat(r.getOccupiedSlots()).isEqualTo(90);
        assertThat(r.getOccupancyRate()).isEqualByComparingTo(new BigDecimal("0.9000"));
        assertThat(r.getWarning()).isTrue();
    }

    @Test
    void listRooms_未达阈值不告警() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(2L);
        room.setRoomNo("402");
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        room.setStatus("active");
        when(warehouseRoomMapper.selectList(any())).thenReturn(List.of(room));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(2L))).thenReturn(50L);

        List<WarehouseRoomResponse> result = service.listRooms(null, null);

        assertThat(result.get(0).getWarning()).isFalse();
        assertThat(result.get(0).getOccupiedSlots()).isEqualTo(50);
    }

    @Test
    void updateRoom_仅修改名称阈值状态() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setRoomNo("401");
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        room.setStatus("active");
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(10L);

        com.archive.dto.request.WarehouseRoomUpdateRequest req =
                new com.archive.dto.request.WarehouseRoomUpdateRequest();
        req.setRoomName("新名称");
        req.setWarningThreshold(new BigDecimal("0.90"));
        req.setStatus("disabled");

        WarehouseRoomResponse resp = service.updateRoom(1L, req);

        assertThat(resp.getRoomName()).isEqualTo("新名称");
        assertThat(resp.getWarningThreshold()).isEqualByComparingTo(new BigDecimal("0.90"));
        assertThat(resp.getStatus()).isEqualTo("disabled");
        verify(warehouseRoomMapper).updateById(any(com.archive.entity.WarehouseRoom.class));
    }

    @Test
    void updateRoom_库房不存在抛404() {
        when(warehouseRoomMapper.selectById(99L)).thenReturn(null);
        com.archive.dto.request.WarehouseRoomUpdateRequest req =
                new com.archive.dto.request.WarehouseRoomUpdateRequest();
        req.setRoomName("x");

        assertThatThrownBy(() -> service.updateRoom(99L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void updateRoom_阈值越界抛校验失败() {
        com.archive.entity.WarehouseRoom room = new com.archive.entity.WarehouseRoom();
        room.setId(1L);
        room.setCapacity(100);
        room.setWarningThreshold(new BigDecimal("0.85"));
        when(warehouseRoomMapper.selectById(1L)).thenReturn(room);
        com.archive.dto.request.WarehouseRoomUpdateRequest req =
                new com.archive.dto.request.WarehouseRoomUpdateRequest();
        req.setWarningThreshold(new BigDecimal("2"));

        assertThatThrownBy(() -> service.updateRoom(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void listLocations_回填当前盒与盒内件数() {
        com.archive.entity.StorageLocation loc1 = new com.archive.entity.StorageLocation();
        loc1.setId(10L);
        loc1.setRoomId(1L);
        loc1.setLocationCode("401-01-01-01");
        loc1.setStatus("active");
        com.archive.entity.StorageLocation loc2 = new com.archive.entity.StorageLocation();
        loc2.setId(11L);
        loc2.setRoomId(1L);
        loc2.setLocationCode("401-01-01-02");
        loc2.setStatus("active");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.archive.entity.StorageLocation> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(loc1, loc2));
        page.setTotal(2L);
        when(storageLocationMapper.selectPage(any(), any())).thenReturn(page);

        com.archive.entity.ArchiveBox box = new com.archive.entity.ArchiveBox();
        box.setId(5L);
        box.setLocationId(10L);
        box.setBoxNo("BOX-000001");
        box.setStatus("normal");
        when(archiveBoxMapper.selectList(any())).thenReturn(List.of(box));
        when(archiveBoxItemMapper.selectCount(any())).thenReturn(3L);

        com.archive.common.PageResult<com.archive.dto.response.StorageLocationResponse> result =
                service.listLocations(1L, null, null, null, 1, 20);

        assertThat(result.getRecords()).hasSize(2);
        com.archive.dto.response.StorageLocationResponse r1 = result.getRecords().get(0);
        assertThat(r1.getOccupied()).isTrue();
        assertThat(r1.getCurrentBoxNo()).isEqualTo("BOX-000001");
        assertThat(r1.getBoxItemCount()).isEqualTo(3);
        com.archive.dto.response.StorageLocationResponse r2 = result.getRecords().get(1);
        assertThat(r2.getOccupied()).isFalse();
        assertThat(r2.getBoxItemCount()).isEqualTo(0);
    }

    @Test
    void updateLocationStatus_停用已占用架位抛冲突() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setStatus("active");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        when(archiveBoxMapper.selectCount(any())).thenReturn(1L); // 已占用

        LocationStatusRequest req = new LocationStatusRequest();
        req.setStatus("disabled");

        assertThatThrownBy(() -> service.updateLocationStatus(10L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void updateLocationStatus_停用空闲架位成功() {
        com.archive.entity.StorageLocation loc = new com.archive.entity.StorageLocation();
        loc.setId(10L);
        loc.setRoomId(1L);
        loc.setLocationCode("401-01-01-01");
        loc.setStatus("active");
        when(storageLocationMapper.selectById(10L)).thenReturn(loc);
        when(archiveBoxMapper.selectCount(any())).thenReturn(0L);
        when(archiveBoxMapper.selectOne(any())).thenReturn(null);

        LocationStatusRequest req = new LocationStatusRequest();
        req.setStatus("disabled");
        req.setReason("架位维修");

        com.archive.dto.response.StorageLocationResponse resp =
                service.updateLocationStatus(10L, req);

        assertThat(resp.getStatus()).isEqualTo("disabled");
        assertThat(resp.getOccupied()).isFalse();
        verify(storageLocationMapper).updateById(any(com.archive.entity.StorageLocation.class));
        verify(auditService).log(eq("M07"), eq("update_location_status"),
                eq("storage_location"), eq(10L), any());
    }
}
