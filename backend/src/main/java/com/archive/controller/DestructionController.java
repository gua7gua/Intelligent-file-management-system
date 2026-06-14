package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.DestructionDestroyRequest;
import com.archive.dto.request.DestructionSubmitRequest;
import com.archive.dto.response.DestructionListDetailResponse;
import com.archive.dto.response.DestructionListResponse;
import com.archive.service.DestructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/destruction-lists")
@RequiredArgsConstructor
@Tag(name = "档案销毁", description = "销毁清册与确认")
public class DestructionController {

    private final DestructionService destructionService;

    @GetMapping
    @Operation(summary = "查询销毁清册")
    public R<PageResult<DestructionListResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(destructionService.listLists(status, keyword, pageNo, pageSize));
    }

    @GetMapping("/{listId}")
    @Operation(summary = "获取销毁清册详情")
    public R<DestructionListDetailResponse> detail(@PathVariable Long listId) {
        return R.ok(destructionService.getListDetail(listId));
    }

    @PostMapping("/{listId}/submit-approval")
    @Operation(summary = "提交销毁审批")
    public R<DestructionListDetailResponse> submitApproval(
            @PathVariable Long listId, @RequestBody @Valid DestructionSubmitRequest req) {
        return R.ok(destructionService.submitApproval(listId, req));
    }

    @PostMapping("/{listId}/photos")
    @Operation(summary = "上传销毁现场照片")
    public R<List<DestructionListDetailResponse.PhotoView>> uploadPhotos(
            @PathVariable Long listId,
            @RequestParam("files") MultipartFile[] files) {
        return R.ok(destructionService.uploadPhotos(listId, files));
    }

    @PostMapping("/{listId}/destroy")
    @Operation(summary = "确认销毁")
    public R<DestructionListDetailResponse> destroy(
            @PathVariable Long listId, @RequestBody @Valid DestructionDestroyRequest req) {
        return R.ok(destructionService.confirmDestroy(listId, req));
    }
}
