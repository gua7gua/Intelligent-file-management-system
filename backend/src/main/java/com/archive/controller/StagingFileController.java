package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.request.StagingFileMatchRequest;
import com.archive.dto.response.StagingFileResponse;
import com.archive.dto.response.StagingFileUploadResponse;
import com.archive.service.StagingFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 暂存电子文件接口。
 */
@RestController
@RequestMapping("/api/admin/reception")
@RequiredArgsConstructor
@Tag(name = "暂存电子文件", description = "电子文件上传、匹配")
public class StagingFileController {

    private final StagingFileService stagingFileService;

    @PostMapping("/batches/{batchId}/staging-files")
    @Operation(summary = "上传暂存电子文件")
    public R<StagingFileUploadResponse> uploadStagingFiles(
            @PathVariable Long batchId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "uploadBatchNo", required = false) String uploadBatchNo) {
        return R.ok(stagingFileService.uploadFiles(batchId, files, uploadBatchNo));
    }

    @PutMapping("/staging-files/{fileId}/match")
    @Operation(summary = "手工匹配暂存文件")
    public R<StagingFileResponse> matchStagingFile(
            @PathVariable Long fileId,
            @Valid @RequestBody StagingFileMatchRequest req) {
        return R.ok(stagingFileService.matchFile(fileId, req));
    }
}
