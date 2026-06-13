package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.request.CollectionRejectRequest;
import com.archive.dto.request.CollectionScheduleRequest;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.service.IntakeBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 征集管理接口（后台）。
 */
@RestController
@RequestMapping("/api/admin/collections")
@RequiredArgsConstructor
@Tag(name = "征集管理", description = "征集联系、拒绝、到馆验收")
public class CollectionManageController {

    private final IntakeBatchService intakeBatchService;

    @GetMapping
    @Operation(summary = "查询征集待办")
    public R<PageResult<IntakeBatchResponse>> listCollections(@Valid BatchPageQuery query) {
        return R.ok(intakeBatchService.listAdminCollections(query));
    }

    @PostMapping("/{batchId}/schedule")
    @Operation(summary = "约定到馆时间")
    public R<IntakeBatchResponse> scheduleReceive(@PathVariable Long batchId,
                                                    @Valid @RequestBody CollectionScheduleRequest req) {
        return R.ok(intakeBatchService.scheduleReceive(batchId, req));
    }

    @PostMapping("/{batchId}/reject")
    @Operation(summary = "拒绝征集意向")
    public R<IntakeBatchResponse> rejectCollection(@PathVariable Long batchId,
                                                     @Valid @RequestBody CollectionRejectRequest req) {
        return R.ok(intakeBatchService.rejectCollection(batchId, req));
    }
}
