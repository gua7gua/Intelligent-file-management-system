package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.ArchiveRequest;
import com.archive.dto.request.BatchShelveRequest;
import com.archive.dto.request.ItemConfirmationRequest;
import com.archive.dto.response.ArchiveResultResponse;
import com.archive.dto.response.PendingBatchDetailResponse;
import com.archive.dto.response.PendingBatchResponse;
import com.archive.dto.response.PendingItemResponse;
import com.archive.dto.response.AiTaskStartResponse;
import com.archive.service.AiTaskService;
import com.archive.service.PendingArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 待入库与上架接口。
 */
@RestController
@RequestMapping("/api/admin/pending-archive")
@RequiredArgsConstructor
@Tag(name = "待入库与上架", description = "待入库批次、确认入库、上架")
public class PendingArchiveController {

    private final PendingArchiveService pendingArchiveService;
    private final AiTaskService aiTaskService;

    @GetMapping("/batches")
    @Operation(summary = "查询待入库批次")
    public R<PageResult<PendingBatchResponse>> listBatches(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(pendingArchiveService.listPendingBatches(
                sourceType, aiStatus, keyword, pageNo, pageSize));
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "获取入库批次详情")
    public R<PendingBatchDetailResponse> getBatchDetail(@PathVariable Long batchId) {
        return R.ok(pendingArchiveService.getBatchDetail(batchId));
    }

    @PutMapping("/items/{itemId}/confirmation")
    @Operation(summary = "确认条目入库字段")
    public R<PendingItemResponse> confirmItemFields(
            @PathVariable Long itemId,
            @RequestBody @Valid ItemConfirmationRequest req) {
        return R.ok(pendingArchiveService.confirmItemFields(itemId, req));
    }

    @PostMapping("/items/{itemId}/archive")
    @Operation(summary = "确认入库")
    public R<ArchiveResultResponse> confirmArchive(
            @PathVariable Long itemId,
            @RequestBody @Valid ArchiveRequest req) {
        return R.ok(pendingArchiveService.confirmArchive(itemId, req));
    }

    @PostMapping("/batches/{batchId}/shelve")
    @Operation(summary = "批次确认上架")
    public R<Void> shelveBatch(
            @PathVariable Long batchId,
            @RequestBody(required = false) BatchShelveRequest req) {
        pendingArchiveService.shelveBatch(batchId,
                req != null ? req : new BatchShelveRequest());
        return R.ok(null);
    }

    @PostMapping("/batches/{batchId}/ai-completion")
    @Operation(summary = "启动 AI 补全")
    public R<AiTaskStartResponse> startAiCompletion(@PathVariable Long batchId) {
        return R.ok(aiTaskService.startIntakeCompletion(batchId));
    }
}
