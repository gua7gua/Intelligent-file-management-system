package com.archive.controller;

import com.archive.common.R;
import com.archive.service.ArchiveFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 档案文件管理接口。
 */
@RestController
@RequestMapping("/api/admin/archive-files")
@RequiredArgsConstructor
@Tag(name = "档案文件管理", description = "文件预览、下载、作废")
public class ArchiveFileController {

    private final ArchiveFileService archiveFileService;

    @GetMapping("/{fileId}/preview")
    @Operation(summary = "档案文件预览")
    public ResponseEntity<Void> preview(@PathVariable Long fileId) {
        String url = archiveFileService.getPreviewUrl(fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "档案文件下载")
    public ResponseEntity<Void> download(@PathVariable Long fileId) {
        String url = archiveFileService.getDownloadUrl(fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
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
