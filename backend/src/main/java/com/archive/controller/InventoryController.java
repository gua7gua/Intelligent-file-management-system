package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.InventoryItemUpdateRequest;
import com.archive.dto.request.InventoryTaskCreateRequest;
import com.archive.dto.request.InventoryTaskQuery;
import com.archive.dto.response.InventoryItemResponse;
import com.archive.dto.response.InventoryTaskDetailResponse;
import com.archive.dto.response.InventoryTaskResponse;
import com.archive.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 档案盘点接口（M12）。方法级全路径 /api/admin/inventory-tasks。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "档案盘点", description = "盘点任务与明细")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/api/admin/inventory-tasks")
    @Operation(summary = "18.1 查询盘点任务")
    public R<PageResult<InventoryTaskResponse>> list(@ModelAttribute InventoryTaskQuery query) {
        return R.ok(inventoryService.list(query));
    }

    @PostMapping("/api/admin/inventory-tasks")
    @Operation(summary = "18.2 创建盘点任务并生成应盘明细")
    public R<InventoryTaskDetailResponse> create(@RequestBody @Valid InventoryTaskCreateRequest req) {
        return R.ok(inventoryService.create(req));
    }

    @PostMapping("/api/admin/inventory-tasks/{taskId}/start")
    @Operation(summary = "18.3 开始盘点")
    public R<InventoryTaskDetailResponse> start(@PathVariable Long taskId) {
        return R.ok(inventoryService.start(taskId));
    }

    @GetMapping("/api/admin/inventory-tasks/{taskId}")
    @Operation(summary = "18.4 获取盘点详情")
    public R<InventoryTaskDetailResponse> detail(@PathVariable Long taskId) {
        return R.ok(inventoryService.getDetail(taskId));
    }

    @PutMapping("/api/admin/inventory-tasks/{taskId}/items/{itemId}")
    @Operation(summary = "18.5 更新盘点明细")
    public R<InventoryItemResponse> updateItem(@PathVariable Long taskId, @PathVariable Long itemId,
                                               @RequestBody @Valid InventoryItemUpdateRequest req) {
        return R.ok(inventoryService.updateItem(taskId, itemId, req));
    }

    @PostMapping("/api/admin/inventory-tasks/{taskId}/complete")
    @Operation(summary = "18.6 完成盘点")
    public R<InventoryTaskDetailResponse> complete(@PathVariable Long taskId,
                                                   @RequestBody(required = false) Map<String, String> body) {
        String summary = body != null ? body.get("summary") : null;
        return R.ok(inventoryService.complete(taskId, summary));
    }
}
