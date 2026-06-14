package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.ArchiveBoxCreateRequest;
import com.archive.dto.request.ArchiveBoxMoveRequest;
import com.archive.dto.request.LocationStatusRequest;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.request.WarehouseRoomUpdateRequest;
import com.archive.dto.response.ArchiveBoxDetailResponse;
import com.archive.dto.response.ArchiveBoxResponse;
import com.archive.dto.response.StorageLocationResponse;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.dto.response.WarehouseWarningResponse;
import com.archive.entity.Archive;
import com.archive.entity.ArchiveBox;
import com.archive.entity.ArchiveBoxItem;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // ==================== 16.3 更新库房 ====================

    public WarehouseRoomResponse updateRoom(Long roomId, WarehouseRoomUpdateRequest req) {
        WarehouseRoom room = warehouseRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库房不存在");
        }
        if (req.getRoomName() != null) {
            room.setRoomName(req.getRoomName());
        }
        if (req.getWarningThreshold() != null) {
            if (req.getWarningThreshold().compareTo(BigDecimal.ZERO) <= 0
                    || req.getWarningThreshold().compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "告警阈值必须在 (0, 1] 范围内");
            }
            room.setWarningThreshold(req.getWarningThreshold());
        }
        if (req.getStatus() != null) {
            room.setStatus(req.getStatus());
        }
        warehouseRoomMapper.updateById(room);
        return toRoomResponse(room, countOccupiedSlots(roomId));
    }

    // ==================== 16.4 查询架位列表 ====================

    public PageResult<StorageLocationResponse> listLocations(
            Long roomId, Integer rackNo, String status, Boolean occupied,
            int pageNo, int pageSize) {

        QueryWrapper<StorageLocation> w = new QueryWrapper<>();
        if (roomId != null) {
            w.eq("room_id", roomId);
        }
        if (rackNo != null) {
            w.eq("rack_no", rackNo);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (occupied != null) {
            String sub = "SELECT location_id FROM archive_boxes WHERE status IN ('normal','full')";
            if (occupied) {
                w.inSql("id", sub);
            } else {
                w.notInSql("id", sub);
            }
        }
        w.orderByAsc("location_code");

        Page<StorageLocation> page = storageLocationMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<StorageLocation> locs = page.getRecords();
        if (locs.isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), pageNo, pageSize, page.getTotal());
        }

        // 批量取本页架位上的活动档案盒
        List<Long> locIds = locs.stream().map(StorageLocation::getId).collect(Collectors.toList());
        QueryWrapper<ArchiveBox> bw = new QueryWrapper<>();
        bw.in("location_id", locIds);
        bw.in("status", java.util.List.of("normal", "full"));
        List<ArchiveBox> boxes = archiveBoxMapper.selectList(bw);
        Map<Long, ArchiveBox> boxByLoc = new HashMap<>();
        for (ArchiveBox box : boxes) {
            boxByLoc.put(box.getLocationId(), box);
        }

        List<StorageLocationResponse> records = locs.stream()
                .map(loc -> toLocationResponse(loc, boxByLoc))
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    private StorageLocationResponse toLocationResponse(StorageLocation loc, Map<Long, ArchiveBox> boxByLoc) {
        StorageLocationResponse resp = new StorageLocationResponse();
        resp.setId(loc.getId());
        resp.setRoomId(loc.getRoomId());
        resp.setRackNo(loc.getRackNo());
        resp.setLayerNo(loc.getLayerNo());
        resp.setBoxSlotNo(loc.getBoxSlotNo());
        resp.setLocationCode(loc.getLocationCode());
        resp.setStatus(loc.getStatus());

        ArchiveBox box = boxByLoc.get(loc.getId());
        resp.setOccupied(box != null);
        if (box != null) {
            resp.setCurrentBoxId(box.getId());
            resp.setCurrentBoxNo(box.getBoxNo());
            QueryWrapper<ArchiveBoxItem> iw = new QueryWrapper<>();
            iw.eq("box_id", box.getId());
            Long c = archiveBoxItemMapper.selectCount(iw);
            resp.setBoxItemCount(c != null ? c.intValue() : 0);
        } else {
            resp.setBoxItemCount(0);
        }
        return resp;
    }

    // ==================== 16.5 停用/启用架位 ====================

    public StorageLocationResponse updateLocationStatus(Long locationId, LocationStatusRequest req) {
        StorageLocation loc = storageLocationMapper.selectById(locationId);
        if (loc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "架位不存在");
        }
        if ("disabled".equals(req.getStatus()) && isLocationOccupied(locationId)) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "已占用架位不得直接停用");
        }
        loc.setStatus(req.getStatus());
        storageLocationMapper.updateById(loc);
        auditService.log("M07", "update_location_status", "storage_location", locationId,
                Map.of("status", req.getStatus(),
                        "reason", req.getReason() != null ? req.getReason() : ""));

        ArchiveBox box = findActiveBoxAt(locationId);
        Map<Long, ArchiveBox> m = new HashMap<>();
        if (box != null) {
            m.put(locationId, box);
        }
        return toLocationResponse(loc, m);
    }

    /** 架位是否被活动档案盒占用。 */
    private boolean isLocationOccupied(Long locationId) {
        QueryWrapper<ArchiveBox> w = new QueryWrapper<>();
        w.eq("location_id", locationId);
        w.in("status", java.util.List.of("normal", "full"));
        return archiveBoxMapper.selectCount(w) > 0;
    }

    /** 取某架位上当前的活动档案盒（至多一个）。 */
    private ArchiveBox findActiveBoxAt(Long locationId) {
        QueryWrapper<ArchiveBox> w = new QueryWrapper<>();
        w.eq("location_id", locationId);
        w.in("status", java.util.List.of("normal", "full"));
        w.last("LIMIT 1");
        return archiveBoxMapper.selectOne(w);
    }

    // ==================== 16.8 新增档案盒 ====================

    public ArchiveBoxResponse createBox(ArchiveBoxCreateRequest req) {
        StorageLocation loc = storageLocationMapper.selectById(req.getLocationId());
        if (loc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "架位不存在");
        }
        if (!"active".equals(loc.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已停用");
        }
        if (isLocationOccupied(req.getLocationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已被占用");
        }

        ArchiveBox box = new ArchiveBox();
        box.setBoxNo(boxNoUtil.generate());
        box.setLocationId(req.getLocationId());
        box.setCategoryId(req.getCategoryId());
        box.setFondsId(req.getFondsId());
        box.setYearLabel(req.getYearLabel());
        box.setSpineText(req.getSpineText());
        box.setCapacity(req.getCapacity());
        box.setUsedCount(0);
        box.setStatus("normal");
        archiveBoxMapper.insert(box);

        auditService.log("M07", "create_box", "archive_box", box.getId(),
                Map.of("boxNo", box.getBoxNo(), "locationId", req.getLocationId()));

        return toBoxResponse(box, loc);
    }

    private ArchiveBoxResponse toBoxResponse(ArchiveBox box, StorageLocation loc) {
        ArchiveBoxResponse resp = new ArchiveBoxResponse();
        resp.setId(box.getId());
        resp.setBoxNo(box.getBoxNo());
        resp.setLocationId(box.getLocationId());
        resp.setCategoryId(box.getCategoryId());
        resp.setFondsId(box.getFondsId());
        resp.setYearLabel(box.getYearLabel());
        resp.setSpineText(box.getSpineText());
        resp.setCapacity(box.getCapacity());
        resp.setUsedCount(box.getUsedCount());
        resp.setStatus(box.getStatus());
        if (loc != null) {
            resp.setLocationCode(loc.getLocationCode());
            WarehouseRoom room = warehouseRoomMapper.selectById(loc.getRoomId());
            if (room != null) {
                resp.setRoomNo(room.getRoomNo());
            }
        }
        return resp;
    }

    // ==================== 16.9 移动档案盒 ====================

    @Transactional
    public ArchiveBoxResponse moveBox(Long boxId, ArchiveBoxMoveRequest req) {
        ArchiveBox box = archiveBoxMapper.selectById(boxId);
        if (box == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案盒不存在");
        }
        if ("destroyed".equals(box.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案盒已销毁，不可移动");
        }
        StorageLocation target = storageLocationMapper.selectById(req.getTargetLocationId());
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标架位不存在");
        }
        if (!"active".equals(target.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已停用");
        }
        if (isLocationOccupied(req.getTargetLocationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "目标架位已被占用");
        }

        box.setLocationId(req.getTargetLocationId());
        archiveBoxMapper.updateById(box);

        auditService.log("M07", "move_box", "archive_box", boxId,
                Map.of("targetLocationId", req.getTargetLocationId(),
                        "reason", req.getReason() != null ? req.getReason() : ""));

        return toBoxResponse(box, target);
    }

    // ==================== 16.6 查询档案盒列表 ====================

    public PageResult<ArchiveBoxResponse> listBoxes(
            String boxNo, Long roomId, Integer categoryId, Long fondsId, String status,
            int pageNo, int pageSize) {

        QueryWrapper<ArchiveBox> w = new QueryWrapper<>();
        if (boxNo != null && !boxNo.isBlank()) {
            w.like("box_no", boxNo);
        }
        if (categoryId != null) {
            w.eq("category_id", categoryId);
        }
        if (fondsId != null) {
            w.eq("fonds_id", fondsId);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (roomId != null) {
            QueryWrapper<StorageLocation> lw = new QueryWrapper<>();
            lw.eq("room_id", roomId);
            lw.select("id");
            List<StorageLocation> roomLocs = storageLocationMapper.selectList(lw);
            List<Long> locIds = roomLocs.stream()
                    .map(StorageLocation::getId).collect(Collectors.toList());
            if (locIds.isEmpty()) {
                return new PageResult<>(java.util.Collections.emptyList(), pageNo, pageSize, 0L);
            }
            w.in("location_id", locIds);
        }
        w.orderByDesc("id");

        Page<ArchiveBox> page = archiveBoxMapper.selectPage(new Page<>(pageNo, pageSize), w);
        List<ArchiveBoxResponse> records = page.getRecords().stream()
                .map(box -> {
                    StorageLocation loc = box.getLocationId() != null
                            ? storageLocationMapper.selectById(box.getLocationId()) : null;
                    return toBoxResponse(box, loc);
                })
                .collect(Collectors.toList());
        return new PageResult<>(records, pageNo, pageSize, page.getTotal());
    }

    // ==================== 16.7 档案盒详情 ====================

    public ArchiveBoxDetailResponse getBoxDetail(Long boxId) {
        ArchiveBox box = archiveBoxMapper.selectById(boxId);
        if (box == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案盒不存在");
        }
        StorageLocation loc = box.getLocationId() != null
                ? storageLocationMapper.selectById(box.getLocationId()) : null;
        WarehouseRoom room = loc != null ? warehouseRoomMapper.selectById(loc.getRoomId()) : null;

        ArchiveBoxDetailResponse resp = new ArchiveBoxDetailResponse();
        resp.setId(box.getId());
        resp.setBoxNo(box.getBoxNo());
        resp.setLocationId(box.getLocationId());
        resp.setCategoryId(box.getCategoryId());
        resp.setFondsId(box.getFondsId());
        resp.setYearLabel(box.getYearLabel());
        resp.setSpineText(box.getSpineText());
        resp.setCapacity(box.getCapacity());
        resp.setUsedCount(box.getUsedCount());
        resp.setStatus(box.getStatus());
        if (loc != null) {
            resp.setLocationCode(loc.getLocationCode());
        }
        if (room != null) {
            resp.setRoomNo(room.getRoomNo());
        }

        // 盒内条目 + 档案信息
        QueryWrapper<ArchiveBoxItem> iw = new QueryWrapper<>();
        iw.eq("box_id", boxId);
        iw.orderByAsc("sort_no");
        List<ArchiveBoxItem> items = archiveBoxItemMapper.selectList(iw);

        List<Long> archiveIds = items.stream()
                .map(ArchiveBoxItem::getArchiveId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Archive> archiveMap = new HashMap<>();
        if (!archiveIds.isEmpty()) {
            List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
            for (Archive a : archives) {
                archiveMap.put(a.getId(), a);
            }
        }

        List<ArchiveBoxDetailResponse.BoxItemView> views = items.stream()
                .map(it -> {
                    ArchiveBoxDetailResponse.BoxItemView v = new ArchiveBoxDetailResponse.BoxItemView();
                    v.setArchiveId(it.getArchiveId());
                    v.setSortNo(it.getSortNo());
                    v.setPageCount(it.getPageCount());
                    v.setPhysicalStatus(it.getPhysicalStatus());
                    Archive a = archiveMap.get(it.getArchiveId());
                    if (a != null) {
                        v.setArchiveNo(a.getArchiveNo());
                        v.setTitle(a.getTitle());
                    }
                    return v;
                })
                .collect(Collectors.toList());
        resp.setItems(views);
        return resp;
    }

    // ==================== 概览：库房告警查询 ====================

    public List<WarehouseWarningResponse> listWarningRooms() {
        QueryWrapper<WarehouseRoom> w = new QueryWrapper<>();
        w.eq("status", "active");
        List<WarehouseRoom> rooms = warehouseRoomMapper.selectList(w);

        List<WarehouseWarningResponse> result = new ArrayList<>();
        for (WarehouseRoom room : rooms) {
            int occupied = countOccupiedSlots(room.getId());
            int capacity = room.getCapacity() != null ? room.getCapacity() : 0;
            BigDecimal rate = capacity > 0
                    ? BigDecimal.valueOf(occupied)
                            .divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal threshold = room.getWarningThreshold() != null
                    ? room.getWarningThreshold() : DEFAULT_WARNING_THRESHOLD;
            if (rate.compareTo(threshold) >= 0) {
                WarehouseWarningResponse r = new WarehouseWarningResponse();
                r.setRoomId(room.getId());
                r.setRoomNo(room.getRoomNo());
                r.setRoomName(room.getRoomName());
                r.setOccupiedSlots(occupied);
                r.setCapacity(capacity);
                r.setOccupancyRate(rate);
                r.setWarningThreshold(threshold);
                result.add(r);
            }
        }
        return result;
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
