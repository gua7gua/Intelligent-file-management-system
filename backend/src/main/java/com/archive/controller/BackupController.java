package com.archive.controller;

import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.BackupTaskCreateRequest;
import com.archive.dto.request.BackupTaskQuery;
import com.archive.dto.response.BackupTaskResponse;
import com.archive.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 档案备份接口（M14 保存）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "档案保存-备份", description = "数据库与文件备份")
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/api/admin/backup-tasks")
    @Operation(summary = "21.1 查询备份任务")
    public R<PageResult<BackupTaskResponse>> list(@ModelAttribute BackupTaskQuery query) {
        return R.ok(backupService.list(query));
    }

    @PostMapping("/api/admin/backup-tasks")
    @Operation(summary = "21.2 创建备份任务")
    public R<BackupTaskResponse> create(@RequestBody @Valid BackupTaskCreateRequest req) {
        return R.ok(backupService.create(req));
    }
}
