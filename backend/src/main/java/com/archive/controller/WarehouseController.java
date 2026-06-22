package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.ArchiveBoxCreateRequest;
import com.archive.dto.request.ArchiveBoxMoveRequest;
import com.archive.dto.request.LocationStatusRequest;
import com.archive.dto.request.WarehouseRoomCreateRequest;
import com.archive.dto.request.WarehouseRoomUpdateRequest;
import com.archive.dto.response.ArchiveBoxDetailResponse;
import com.archive.dto.response.ArchiveBoxResponse;
import com.archive.dto.response.StorageLocationResponse;
import com.archive.dto.response.WarehouseRoomResponse;
import com.archive.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库房管理接口。
 */
@RestController
@RequestMapping("/api/admin/warehouse")
@RequiredArgsConstructor
@Tag(name = "库房管理", description = "库房、架位、档案盒管理")
public class WarehouseController {

    private final WarehouseService warehouseService;

    // ==================== 16.1 查询库房列表 ====================

    @GetMapping("/rooms")
    @Operation(summary = "查询库房列表")
    public R<List<WarehouseRoomResponse>> listRooms(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(warehouseService.listRooms(status, keyword));
    }

    // ==================== 16.2 新增库房 ====================

    @PostMapping("/rooms")
    @Operation(summary = "新增库房")
    public R<WarehouseRoomResponse> createRoom(@RequestBody @Valid WarehouseRoomCreateRequest req) {
        return R.ok(warehouseService.createRoom(req));
    }

    // ==================== 16.3 更新库房 ====================

    @PutMapping("/rooms/{roomId}")
    @Operation(summary = "更新库房")
    public R<WarehouseRoomResponse> updateRoom(
            @PathVariable Long roomId,
            @RequestBody WarehouseRoomUpdateRequest req) {
        return R.ok(warehouseService.updateRoom(roomId, req));
    }

    // ==================== 16.10 删除库房 ====================

    @DeleteMapping("/rooms/{roomId}")
    @Operation(summary = "删除库房（无活动档案盒时允许；历史盒解除架位归属）")
    public R<Void> deleteRoom(@PathVariable Long roomId) {
        warehouseService.deleteRoom(roomId);
        return R.ok();
    }

    // ==================== 16.4 查询架位 ====================

    @GetMapping("/locations")
    @Operation(summary = "查询架位列表")
    public R<PageResult<StorageLocationResponse>> listLocations(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer rackNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean occupied,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(warehouseService.listLocations(roomId, rackNo, status, occupied, pageNo, pageSize));
    }

    // ==================== 16.5 停用/启用架位 ====================

    @PutMapping("/locations/{locationId}/status")
    @Operation(summary = "停用或启用架位")
    public R<StorageLocationResponse> updateLocationStatus(
            @PathVariable Long locationId,
            @RequestBody @Valid LocationStatusRequest req) {
        return R.ok(warehouseService.updateLocationStatus(locationId, req));
    }

    // ==================== 16.6 查询档案盒 ====================

    @GetMapping("/boxes")
    @Operation(summary = "查询档案盒列表")
    public R<PageResult<ArchiveBoxResponse>> listBoxes(
            @RequestParam(required = false) String boxNo,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Long fondsId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(warehouseService.listBoxes(boxNo, roomId, categoryId, fondsId, status, pageNo, pageSize));
    }

    // ==================== 16.7 档案盒详情 ====================

    @GetMapping("/boxes/{boxId}")
    @Operation(summary = "获取档案盒详情")
    public R<ArchiveBoxDetailResponse> getBoxDetail(@PathVariable Long boxId) {
        return R.ok(warehouseService.getBoxDetail(boxId));
    }

    // ==================== 16.8 新增档案盒 ====================

    @PostMapping("/boxes")
    @Operation(summary = "新增档案盒")
    public R<ArchiveBoxResponse> createBox(@RequestBody @Valid ArchiveBoxCreateRequest req) {
        return R.ok(warehouseService.createBox(req));
    }

    // ==================== 16.9 移动档案盒 ====================

    @PostMapping("/boxes/{boxId}/move")
    @Operation(summary = "移动档案盒")
    public R<ArchiveBoxResponse> moveBox(
            @PathVariable Long boxId,
            @RequestBody @Valid ArchiveBoxMoveRequest req) {
        return R.ok(warehouseService.moveBox(boxId, req));
    }

    // ==================== 16.10 删除空档案盒（释放架位） ====================

    @DeleteMapping("/boxes/{boxId}")
    @Operation(summary = "删除空档案盒，释放所在架位")
    public R<Void> deleteBox(@PathVariable Long boxId) {
        warehouseService.deleteBox(boxId);
        return R.ok();
    }
}
