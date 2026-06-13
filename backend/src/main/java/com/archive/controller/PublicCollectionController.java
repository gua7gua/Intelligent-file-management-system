package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BatchPageQuery;
import com.archive.dto.request.CollectionBatchCreateRequest;
import com.archive.dto.request.CollectionSubmitRequest;
import com.archive.dto.response.IntakeBatchResponse;
import com.archive.service.IntakeBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公众征集清单接口。
 */
@RestController
@RequestMapping("/api/public/collections")
@RequiredArgsConstructor
@Tag(name = "公众征集", description = "公众端征集清单 CRUD、提交")
public class PublicCollectionController {

    private final IntakeBatchService intakeBatchService;

    @GetMapping
    @Operation(summary = "查询本人征集清单")
    public R<PageResult<IntakeBatchResponse>> listCollections(@Valid BatchPageQuery query) {
        return R.ok(intakeBatchService.listCollections(query));
    }

    @PostMapping
    @Operation(summary = "创建征集清单草稿")
    public R<IntakeBatchResponse> createCollection(@Valid @RequestBody CollectionBatchCreateRequest req) {
        return R.ok(intakeBatchService.createCollection(req));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "获取征集清单详情")
    public R<IntakeBatchResponse> getCollection(@PathVariable Long batchId) {
        return R.ok(intakeBatchService.getCollection(batchId));
    }

    @PutMapping("/{batchId}")
    @Operation(summary = "更新征集草稿")
    public R<IntakeBatchResponse> updateCollection(@PathVariable Long batchId,
                                                     @Valid @RequestBody CollectionBatchCreateRequest req) {
        return R.ok(intakeBatchService.updateCollection(batchId, req));
    }

    @PostMapping("/{batchId}/submit")
    @Operation(summary = "提交征集清单")
    public R<IntakeBatchResponse> submitCollection(@PathVariable Long batchId,
                                                     @Valid @RequestBody CollectionSubmitRequest req) {
        return R.ok(intakeBatchService.submitCollection(batchId, req));
    }
}
