package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AppraisalBatchCreateRequest;
import com.archive.dto.request.AppraisalItemSaveRequest;
import com.archive.dto.response.AppraisalBatchDetailResponse;
import com.archive.dto.response.AppraisalBatchResponse;
import com.archive.dto.response.AppraisalStatsResponse;
import com.archive.service.AppraisalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appraisal-batches")
@RequiredArgsConstructor
@Tag(name = "档案鉴定", description = "鉴定批次与明细")
public class AppraisalController {

    private final AppraisalService appraisalService;

    @GetMapping
    @Operation(summary = "查询鉴定批次")
    public R<PageResult<AppraisalBatchResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer formedYearStart,
            @RequestParam(required = false) Integer formedYearEnd,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(appraisalService.listBatches(status, categoryId, formedYearStart, formedYearEnd, pageNo, pageSize));
    }

    @GetMapping("/stats")
    @Operation(summary = "鉴定工作台顶部统计（真实聚合）")
    public R<AppraisalStatsResponse> stats() {
        return R.ok(appraisalService.stats());
    }

    @PostMapping
    @Operation(summary = "创建鉴定批次")
    public R<AppraisalBatchDetailResponse> create(@RequestBody @Valid AppraisalBatchCreateRequest req) {
        return R.ok(appraisalService.createBatch(req));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "获取鉴定批次详情")
    public R<AppraisalBatchDetailResponse> detail(@PathVariable Long batchId) {
        return R.ok(appraisalService.getBatchDetail(batchId));
    }

    @PutMapping("/{batchId}/items")
    @Operation(summary = "保存鉴定明细")
    public R<AppraisalBatchDetailResponse> saveItems(
            @PathVariable Long batchId, @RequestBody @Valid AppraisalItemSaveRequest req) {
        return R.ok(appraisalService.saveItems(batchId, req));
    }

    @PostMapping("/{batchId}/complete")
    @Operation(summary = "完成鉴定")
    public R<AppraisalBatchDetailResponse> complete(@PathVariable Long batchId) {
        return R.ok(appraisalService.completeBatch(batchId));
    }
}
