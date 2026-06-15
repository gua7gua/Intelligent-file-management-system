package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.CursorResult;
import com.archive.common.ErrorCode;
import com.archive.common.R;
import com.archive.dto.request.ArchiveAccessLogQuery;
import com.archive.dto.response.ArchiveAccessLogResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.ArchiveAccessLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/archive-access-logs")
@RequiredArgsConstructor
@Tag(name = "档案访问日志", description = "档案访问日志查询（后台档案管理员、系统管理员，只读）；导出由前端 CSV 完成")
public class ArchiveAccessLogController {

    private final ArchiveAccessLogQueryService archiveAccessLogQueryService;

    @GetMapping
    @Operation(summary = "查询档案访问日志（游标分页）")
    public R<CursorResult<ArchiveAccessLogResponse>> list(@Valid ArchiveAccessLogQuery query) {
        requireBackOrAdmin();
        return R.ok(archiveAccessLogQueryService.query(query));
    }

    private void requireBackOrAdmin() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
