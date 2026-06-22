package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BatchCompleteRequest;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.request.ItemAcceptanceRequest;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.dto.response.IntakeItemResponse;
import com.archive.service.IntakeBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 前台移交验收接口。
 */
@RestController
@RequestMapping("/api/admin/reception")
@RequiredArgsConstructor
@Tag(name = "前台移交验收", description = "前台验收、回退、回执")
public class ReceptionController {

    private final IntakeBatchService intakeBatchService;

    @GetMapping("/batches")
    @Operation(summary = "查询待验收批次")
    public R<PageResult<IntakeBatchResponse>> listPendingBatches(@Valid BatchPageQuery query) {
        return R.ok(intakeBatchService.listPendingReception(query));
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "获取验收详情")
    public R<IntakeBatchResponse> getReceptionDetail(@PathVariable Long batchId) {
        return R.ok(intakeBatchService.getReceptionDetail(batchId));
    }

    @PutMapping("/items/{itemId}/acceptance")
    @Operation(summary = "条目验收/回退")
    public R<IntakeItemResponse> acceptItem(@PathVariable Long itemId,
                                             @Valid @RequestBody ItemAcceptanceRequest req) {
        return R.ok(intakeBatchService.acceptItem(itemId, req));
    }

    @PostMapping("/batches/{batchId}/complete")
    @Operation(summary = "完成批次验收")
    public R<IntakeBatchResponse> completeAcceptance(@PathVariable Long batchId,
                                                      @RequestBody(required = false) BatchCompleteRequest req) {
        return R.ok(intakeBatchService.completeAcceptance(batchId, req));
    }

    @GetMapping("/batches/{batchId}/receipt")
    @Operation(summary = "导出接收回执")
    public ResponseEntity<byte[]> exportReceipt(@PathVariable Long batchId) {
        byte[] data = intakeBatchService.exportReceipt(batchId);
        // 文件名带批次号中文化（P2-1）：接收回执-BAT-000012.pdf
        String batchNo = String.valueOf(batchId);
        try {
            IntakeBatchResponse resp = intakeBatchService.getReceptionDetail(batchId);
            if (resp != null && resp.getBatchNo() != null) batchNo = resp.getBatchNo();
        } catch (Exception ignored) { }
        String filename = URLEncoder.encode("接收回执-" + batchNo + ".pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
