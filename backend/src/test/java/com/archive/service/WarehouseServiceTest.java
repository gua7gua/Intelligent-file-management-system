package com.archive.service;

import com.archive.common.ErrorCode;
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
}
