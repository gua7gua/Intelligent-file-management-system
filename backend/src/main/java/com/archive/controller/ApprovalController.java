package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.ApprovalOpinionRequest;
import com.archive.dto.response.ApprovalDetailResponse;
import com.archive.dto.response.ApprovalResponse;
import com.archive.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
@Tag(name = "审批工作台", description = "密级/开放/销毁审批")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    @Operation(summary = "查询审批单")
    public R<PageResult<ApprovalResponse>> list(
            @RequestParam(required = false) String approvalType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(approvalService.listApprovals(approvalType, status, keyword, pageNo, pageSize));
    }

    @GetMapping("/{approvalId}")
    @Operation(summary = "获取审批详情")
    public R<ApprovalDetailResponse> detail(@PathVariable Long approvalId) {
        return R.ok(approvalService.getApprovalDetail(approvalId));
    }

    @PostMapping("/{approvalId}/approve")
    @Operation(summary = "审批通过")
    public R<ApprovalDetailResponse> approve(
            @PathVariable Long approvalId, @RequestBody ApprovalOpinionRequest req) {
        return R.ok(approvalService.approve(approvalId, req));
    }

    @PostMapping("/{approvalId}/reject")
    @Operation(summary = "审批退回")
    public R<ApprovalDetailResponse> reject(
            @PathVariable Long approvalId, @RequestBody ApprovalOpinionRequest req) {
        return R.ok(approvalService.reject(approvalId, req));
    }
}
