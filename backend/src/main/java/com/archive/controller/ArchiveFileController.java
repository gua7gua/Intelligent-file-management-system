package com.archive.controller;

import com.archive.common.FileResponseHelper;
import com.archive.common.R;
import com.archive.entity.ArchiveFile;
import com.archive.service.ArchiveFileService;
import com.archive.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 档案文件管理接口。
 * <p>
 * 预览/下载走后端代理流（读取 minio 对象流以 Blob 返回前端），不暴露预签名 URL，
 * 避免浏览器无法解析容器内地址 minio:9000。
 */
@RestController
@RequestMapping("/api/admin/archive-files")
@RequiredArgsConstructor
@Tag(name = "档案文件管理", description = "文件预览、下载、作废")
public class ArchiveFileController {

    private final ArchiveFileService archiveFileService;
    private final MinioService minioService;

    @GetMapping("/{fileId}/preview")
    @Operation(summary = "档案文件预览")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long fileId) {
        ArchiveFile file = archiveFileService.getFileForPreview(fileId);
        return FileResponseHelper.stream(minioService, file, true);
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "档案文件下载")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId) {
        ArchiveFile file = archiveFileService.getFileForDownload(fileId);
        return FileResponseHelper.stream(minioService, file, false);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除或作废档案文件")
    public R<Boolean> deleteFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String reason) {
        archiveFileService.deleteFile(fileId, reason);
        return R.ok(true);
    }
}
