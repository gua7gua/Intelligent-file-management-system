package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.entity.StorageLocation;
import com.archive.entity.WarehouseRoom;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveBoxItemMapper;
import com.archive.mapper.ArchiveBoxMapper;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.StorageLocationMapper;
import com.archive.mapper.WarehouseRoomMapper;
import com.archive.util.BoxNoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 库房管理服务。
 * 处理库房、架位、档案盒的增删改查、移动与占用率告警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRoomMapper warehouseRoomMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final ArchiveBoxMapper archiveBoxMapper;
    private final ArchiveBoxItemMapper archiveBoxItemMapper;
    private final ArchiveMapper archiveMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BoxNoUtil boxNoUtil;
    private final AuditService auditService;

    private static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal("0.85");

    /** 按库房统计已占用盒位数（架位上有活动档案盒）。 */
    private static final String COUNT_OCCUPIED_BY_ROOM_SQL =
            "SELECT COUNT(*) FROM archive_boxes " +
            "WHERE location_id IN (SELECT id FROM storage_locations WHERE room_id = ?) " +
            "AND status IN ('normal','full')";

    // ==================== 16.2 新增库房 ====================

    @Transactional
    public WarehouseRoomResponse createRoom(WarehouseRoomCreateRequest req) {
        // 1. roomNo 唯一
        QueryWrapper<WarehouseRoom> rw = new QueryWrapper<>();
        rw.eq("room_no", req.getRoomNo());
        if (warehouseRoomMapper.selectCount(rw) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "库房号已存在");
        }

        // 2. 阈值默认与范围校验
        BigDecimal threshold = req.getWarningThreshold() != null
                ? req.getWarningThreshold() : DEFAULT_WARNING_THRESHOLD;
        if (threshold.compareTo(BigDecimal.ZERO) <= 0
                || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "告警阈值必须在 (0, 1] 范围内");
        }

        // 3. 容量
        int capacity = req.getRackCount() * req.getLayersPerRack() * req.getBoxesPerLayer();

        WarehouseRoom room = new WarehouseRoom();
        room.setRoomNo(req.getRoomNo());
        room.setRoomName(req.getRoomName());
        room.setRackCount(req.getRackCount());
        room.setLayersPerRack(req.getLayersPerRack());
        room.setBoxesPerLayer(req.getBoxesPerLayer());
        room.setCapacity(capacity);
        room.setWarningThreshold(threshold);
        room.setStatus("active");
        warehouseRoomMapper.insert(room);

        // 4. 批量生成架位
        generateLocations(room);

        auditService.log("M07", "create_room", "warehouse_room", room.getId(),
                Map.of("roomNo", req.getRoomNo(), "capacity", capacity));

        return toRoomResponse(room, 0);
    }

    // ==================== 16.1 查询库房列表 ====================

    public List<WarehouseRoomResponse> listRooms(String status, String keyword) {
        QueryWrapper<WarehouseRoom> w = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like("room_no", keyword).or().like("room_name", keyword));
        }
        w.orderByAsc("room_no");

        List<WarehouseRoom> rooms = warehouseRoomMapper.selectList(w);
        return rooms.stream()
                .map(room -> toRoomResponse(room, countOccupiedSlots(room.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 统计某库房当前被活动档案盒占用的盒位数。 */
    private int countOccupiedSlots(Long roomId) {
        Long count = jdbcTemplate.queryForObject(
                COUNT_OCCUPIED_BY_ROOM_SQL, Long.class, roomId);
        return count != null ? count.intValue() : 0;
    }

    /** 按库房结构生成固定架位，编码 {roomNo}-{rack:02d}-{layer:02d}-{slot:02d}。 */
    private void generateLocations(WarehouseRoom room) {
        List<StorageLocation> locations = new ArrayList<>();
        for (int rack = 1; rack <= room.getRackCount(); rack++) {
            for (int layer = 1; layer <= room.getLayersPerRack(); layer++) {
                for (int slot = 1; slot <= room.getBoxesPerLayer(); slot++) {
                    StorageLocation loc = new StorageLocation();
                    loc.setRoomId(room.getId());
                    loc.setRackNo(rack);
                    loc.setLayerNo(layer);
                    loc.setBoxSlotNo(slot);
                    loc.setLocationCode(String.format("%s-%02d-%02d-%02d",
                            room.getRoomNo(), rack, layer, slot));
                    loc.setStatus("active");
                    locations.add(loc);
                }
            }
        }
        for (StorageLocation loc : locations) {
            storageLocationMapper.insert(loc);
        }
    }

    /** 组装库房响应，occupiedSlots 由调用方提供（listRooms 时实时计算）。 */
    private WarehouseRoomResponse toRoomResponse(WarehouseRoom room, int occupiedSlots) {
        WarehouseRoomResponse resp = new WarehouseRoomResponse();
        resp.setId(room.getId());
        resp.setRoomNo(room.getRoomNo());
        resp.setRoomName(room.getRoomName());
        resp.setRackCount(room.getRackCount());
        resp.setLayersPerRack(room.getLayersPerRack());
        resp.setBoxesPerLayer(room.getBoxesPerLayer());
        resp.setCapacity(room.getCapacity());
        resp.setWarningThreshold(room.getWarningThreshold());
        resp.setStatus(room.getStatus());
        resp.setOccupiedSlots(occupiedSlots);
        int capacity = room.getCapacity() != null ? room.getCapacity() : 0;
        BigDecimal rate = capacity > 0
                ? BigDecimal.valueOf(occupiedSlots)
                        .divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        resp.setOccupancyRate(rate);
        BigDecimal threshold = room.getWarningThreshold() != null
                ? room.getWarningThreshold() : DEFAULT_WARNING_THRESHOLD;
        resp.setWarning(rate.compareTo(threshold) >= 0);
        return resp;
    }
}
