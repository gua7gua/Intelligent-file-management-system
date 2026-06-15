package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AiQueryRequest;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.dto.response.PublicDashboardResponse;
import com.archive.exception.BusinessException;
import com.archive.service.PublicDashboardService;
import com.archive.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 公众检索接口（5.2-5.6）。
 * /api/public/** 在 SaTokenConfig 免登录；5.5 下载在 Service 层校验登录。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "公众检索", description = "公开检索、详情、预览、下载、AI 检索")
public class PublicSearchController {

    private final SearchService searchService;
    private final PublicDashboardService publicDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "公众概览（需登录公众账号）")
    public R<PublicDashboardResponse> dashboard() {
        // /api/public/** 免登录，但概览需登录态取本人数据
        if (!AuthContext.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return R.ok(publicDashboardService.overview());
    }

    @GetMapping("/archives/search")
    @Operation(summary = "公开档案检索")
    public R<PageResult<ArchiveSummaryResponse>> search(@ModelAttribute ArchiveSearchQuery query) {
        return R.ok(searchService.publicSearch(query));
    }

    @GetMapping("/archives/{archiveId}")
    @Operation(summary = "公开档案详情")
    public R<ArchiveSearchDetailResponse> detail(@PathVariable Long archiveId, HttpServletRequest req) {
        Long uid = currentPublicUserId();
        return R.ok(searchService.publicDetail(archiveId, uid, uid != null ? "public" : "anonymous", req));
    }

    @GetMapping("/archive-files/{fileId}/preview")
    @Operation(summary = "公开档案预览")
    public ResponseEntity<Void> preview(@PathVariable Long fileId, HttpServletRequest req) {
        Long uid = currentPublicUserId();
        String url = searchService.publicPreview(fileId, uid, uid != null ? "public" : "anonymous", req);
        return redirect(url);
    }

    @GetMapping("/archive-files/{fileId}/download")
    @Operation(summary = "公开档案下载")
    public ResponseEntity<Void> download(@PathVariable Long fileId, HttpServletRequest req) {
        Long uid = currentPublicUserId();
        // publicDownload 内部校验 uid==null 抛 UNAUTHORIZED
        String url = searchService.publicDownload(fileId, uid, uid != null ? "public" : "anonymous", req);
        return redirect(url);
    }

    @PostMapping("/archives/ai-query")
    @Operation(summary = "公众 AI 检索 JSON 生成")
    public R<AiQueryResponse> aiQuery(@RequestBody @Valid AiQueryRequest req) {
        return R.ok(searchService.publicAiQuery(req.getText()));
    }

    /** 公众端可能未登录；未登录返回 null。 */
    private Long currentPublicUserId() {
        try {
            return AuthContext.isAuthenticated() ? AuthContext.getCurrentUserId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<Void> redirect(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
