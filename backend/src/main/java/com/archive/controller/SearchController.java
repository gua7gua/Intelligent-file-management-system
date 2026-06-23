package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.FileResponseHelper;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.AiQueryRequest;
import com.archive.dto.request.ArchiveSearchQuery;
import com.archive.dto.response.AiQueryResponse;
import com.archive.dto.response.ArchiveSearchDetailResponse;
import com.archive.dto.response.ArchiveSummaryResponse;
import com.archive.dto.response.InternalDashboardResponse;
import com.archive.entity.ArchiveFile;
import com.archive.service.MinioService;
import com.archive.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 内部查阅者检索接口（11.1-11.6）。
 * /api/internal/** 需登录（SaTokenConfig 默认拦截 /api/**）。
 * <p>
 * 预览/下载走后端代理流（minio 对象流以 Blob 返回），不暴露预签名 URL。
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "内部检索", description = "内部工作台、检索、详情、预览、下载、AI 检索")
public class SearchController {

    private final SearchService searchService;
    private final MinioService minioService;

    @GetMapping("/dashboard")
    @Operation(summary = "内部工作台")
    public R<InternalDashboardResponse> dashboard() {
        return R.ok(searchService.getInternalDashboard(AuthContext.getCurrentUserId()));
    }

    @GetMapping("/archives/search")
    @Operation(summary = "内部档案检索")
    public R<PageResult<ArchiveSummaryResponse>> search(@ModelAttribute ArchiveSearchQuery query) {
        // internalSearch 内部读取 AuthContext 做权限过滤
        return R.ok(searchService.internalSearch(query));
    }

    @GetMapping("/archives/{archiveId}")
    @Operation(summary = "内部档案详情")
    public R<ArchiveSearchDetailResponse> detail(@PathVariable Long archiveId, HttpServletRequest req) {
        return R.ok(searchService.internalDetail(archiveId,
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId(), AuthContext.getCurrentUserId(), req));
    }

    @GetMapping("/archive-files/{fileId}/preview")
    @Operation(summary = "内部预览电子文件")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long fileId, HttpServletRequest req) {
        ArchiveFile file = searchService.internalPreview(fileId,
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId(), AuthContext.getCurrentUserId(), req);
        return FileResponseHelper.stream(minioService, file, true);
    }

    @GetMapping("/archive-files/{fileId}/download")
    @Operation(summary = "内部下载电子文件")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId, HttpServletRequest req) {
        ArchiveFile file = searchService.internalDownload(fileId,
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId(), AuthContext.getCurrentUserId(), req);
        return FileResponseHelper.stream(minioService, file, false);
    }

    @PostMapping("/archives/ai-query")
    @Operation(summary = "内部 AI 检索 JSON 生成")
    public R<AiQueryResponse> aiQuery(@RequestBody @Valid AiQueryRequest req) {
        return R.ok(searchService.internalAiQuery(req.getText(),
                AuthContext.getMaxSecurityLevel(), AuthContext.getDataScope(),
                AuthContext.getOrganizationId()));
    }
}
