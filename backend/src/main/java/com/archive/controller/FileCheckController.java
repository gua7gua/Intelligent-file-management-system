package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.FileCheckRecordQuery;
import com.archive.dto.request.FileCheckTriggerRequest;
import com.archive.dto.response.FileCheckRecordResponse;
import com.archive.service.FileCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 四性检测接口（M14 保存）。跨 /api/admin/file-check-records 与 /api/admin/archive-files 路径。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "档案保存-四性检测", description = "完整性/可用性/真实性/安全性")
public class FileCheckController {

    private final FileCheckService fileCheckService;

    @GetMapping("/api/admin/file-check-records")
    @Operation(summary = "21.3 查询四性检测记录")
    public R<PageResult<FileCheckRecordResponse>> list(@ModelAttribute FileCheckRecordQuery query) {
        return R.ok(fileCheckService.list(query));
    }

    @PostMapping("/api/admin/archive-files/{fileId}/checks")
    @Operation(summary = "21.4 触发正式文件检测")
    public R<List<FileCheckRecordResponse>> trigger(@PathVariable Long fileId,
                                                    @RequestBody @Valid FileCheckTriggerRequest req) {
        return R.ok(fileCheckService.trigger(fileId, req));
    }
}
