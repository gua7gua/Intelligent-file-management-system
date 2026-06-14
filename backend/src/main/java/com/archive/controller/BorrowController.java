package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BorrowApplyRequest;
import com.archive.dto.request.BorrowApproveRequest;
import com.archive.dto.request.BorrowCheckoutRequest;
import com.archive.dto.request.BorrowRequestQuery;
import com.archive.dto.request.BorrowReturnRequest;
import com.archive.dto.response.BorrowRequestResponse;
import com.archive.service.BorrowService;
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
 * 借阅管理接口（M09）。方法级全路径，跨 /api/internal 与 /api/admin 两个 base path。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "借阅管理", description = "借阅申请、审批、出库、归还")
public class BorrowController {

    private final BorrowService borrowService;

    // ==================== 11.7 提交借阅申请 ====================

    @PostMapping("/api/internal/borrow-requests")
    @Operation(summary = "提交借阅申请")
    public R<BorrowRequestResponse> apply(@RequestBody @Valid BorrowApplyRequest req) {
        return R.ok(borrowService.apply(req));
    }

    // ==================== 11.8 查询我的借阅申请 ====================

    @GetMapping("/api/internal/borrow-requests")
    @Operation(summary = "查询我的借阅申请")
    public R<PageResult<BorrowRequestResponse>> listMine(@ModelAttribute BorrowRequestQuery query) {
        return R.ok(borrowService.listMine(query));
    }

    // ==================== 11.9 借阅申请详情（本人） ====================

    @GetMapping("/api/internal/borrow-requests/{requestId}")
    @Operation(summary = "我的借阅申请详情")
    public R<BorrowRequestResponse> getMine(@PathVariable Long requestId) {
        return R.ok(borrowService.getMine(requestId));
    }

    // ==================== 11.10 导出借阅凭证 ====================

    @GetMapping("/api/internal/borrow-requests/{requestId}/voucher")
    @Operation(summary = "导出借阅凭证 PDF")
    public ResponseEntity<byte[]> exportVoucher(@PathVariable Long requestId) {
        byte[] pdf = borrowService.exportVoucher(requestId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = URLEncoder.encode("借阅凭证.pdf", StandardCharsets.UTF_8);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ==================== 12.1 管理端查询借阅申请 ====================

    @GetMapping("/api/admin/borrow-requests")
    @Operation(summary = "管理端查询借阅申请")
    public R<PageResult<BorrowRequestResponse>> adminList(@ModelAttribute BorrowRequestQuery query) {
        return R.ok(borrowService.adminList(query));
    }

    // ==================== 12.2 管理端申请详情 ====================

    @GetMapping("/api/admin/borrow-requests/{requestId}")
    @Operation(summary = "管理端借阅申请详情")
    public R<BorrowRequestResponse> adminGet(@PathVariable Long requestId) {
        return R.ok(borrowService.adminGet(requestId));
    }

    // ==================== 12.3 审批借阅申请 ====================

    @PostMapping("/api/admin/borrow-requests/{requestId}/approve")
    @Operation(summary = "审批借阅申请")
    public R<BorrowRequestResponse> approve(@PathVariable Long requestId,
                                            @RequestBody @Valid BorrowApproveRequest req) {
        return R.ok(borrowService.approve(requestId, req));
    }

    // ==================== 12.4 核验凭证并确认出库 ====================

    @PostMapping("/api/admin/borrow-requests/{requestId}/checkout")
    @Operation(summary = "核验凭证并确认出库")
    public R<BorrowRequestResponse> checkout(@PathVariable Long requestId,
                                             @RequestBody @Valid BorrowCheckoutRequest req) {
        return R.ok(borrowService.checkout(requestId, req));
    }

    // ==================== 12.5 确认归还 ====================

    @PostMapping("/api/admin/borrow-requests/{requestId}/return")
    @Operation(summary = "确认归还")
    public R<BorrowRequestResponse> returnBorrow(@PathVariable Long requestId,
                                                 @RequestBody @Valid BorrowReturnRequest req) {
        return R.ok(borrowService.returnBorrow(requestId, req));
    }
}
