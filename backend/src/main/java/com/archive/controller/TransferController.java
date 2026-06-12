package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.request.TransferBatchCreateRequest;
import com.archive.dto.request.TransferItemRequest;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.IntakeItemResponse;
import com.archive.dto.response.TransferDashboardResponse;
import com.archive.service.IntakeBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 移交单位门户接口。
 */
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Tag(name = "移交单位门户", description = "移交清单 CRUD、提交、导出")
public class TransferController {

    private final IntakeBatchService intakeBatchService;

    @GetMapping("/dashboard")
    @Operation(summary = "移交工作台统计")
    public R<TransferDashboardResponse> dashboard() {
        return R.ok(intakeBatchService.transferDashboard());
    }

    @GetMapping("/batches")
    @Operation(summary = "查询移交清单列表")
    public R<PageResult<IntakeBatchResponse>> listBatches(@Valid BatchPageQuery query) {
        return R.ok(intakeBatchService.listTransferBatches(query));
    }

    @PostMapping("/batches")
    @Operation(summary = "创建移交清单草稿")
    public R<IntakeBatchResponse> createBatch(@Valid @RequestBody TransferBatchCreateRequest req) {
        return R.ok(intakeBatchService.createTransferBatch(req));
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "获取移交清单详情")
    public R<IntakeBatchResponse> getBatch(@PathVariable Long batchId) {
        return R.ok(intakeBatchService.getTransferBatch(batchId));
    }

    @PutMapping("/batches/{batchId}")
    @Operation(summary = "更新移交清单草稿")
    public R<IntakeBatchResponse> updateBatch(@PathVariable Long batchId,
                                               @Valid @RequestBody TransferBatchCreateRequest req) {
        return R.ok(intakeBatchService.updateTransferBatch(batchId, req));
    }

    @DeleteMapping("/batches/{batchId}")
    @Operation(summary = "删除移交清单草稿")
    public R<Void> deleteBatch(@PathVariable Long batchId) {
        intakeBatchService.deleteTransferBatch(batchId);
        return R.ok();
    }

    @PostMapping("/batches/{batchId}/items")
    @Operation(summary = "新增移交清单条目")
    public R<IntakeItemResponse> addItem(@PathVariable Long batchId,
                                          @Valid @RequestBody TransferItemRequest req) {
        return R.ok(intakeBatchService.addTransferItem(batchId, req));
    }

    @PutMapping("/batches/{batchId}/items/{itemId}")
    @Operation(summary = "更新移交清单条目")
    public R<IntakeItemResponse> updateItem(@PathVariable Long batchId,
                                             @PathVariable Long itemId,
                                             @Valid @RequestBody TransferItemRequest req) {
        return R.ok(intakeBatchService.updateTransferItem(batchId, itemId, req));
    }

    @DeleteMapping("/batches/{batchId}/items/{itemId}")
    @Operation(summary = "删除移交清单条目")
    public R<Void> deleteItem(@PathVariable Long batchId,
                               @PathVariable Long itemId) {
        intakeBatchService.deleteTransferItem(batchId, itemId);
        return R.ok();
    }

    @PostMapping("/batches/{batchId}/submit")
    @Operation(summary = "提交移交清单")
    public R<IntakeBatchResponse> submitBatch(@PathVariable Long batchId) {
        return R.ok(intakeBatchService.submitTransferBatch(batchId));
    }

    @GetMapping("/batches/{batchId}/export")
    @Operation(summary = "导出移交清单 PDF（骨架）")
    public ResponseEntity<byte[]> exportBatch(@PathVariable Long batchId) {
        byte[] data = intakeBatchService.exportReceipt(batchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transfer.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
