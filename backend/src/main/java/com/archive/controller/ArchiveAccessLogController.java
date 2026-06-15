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
import com.archive.util.LogExcelExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/archive-access-logs")
@RequiredArgsConstructor
@Tag(name = "档案访问日志", description = "档案访问日志查询/导出（后台档案管理员、系统管理员，只读）")
public class ArchiveAccessLogController {

    private final ArchiveAccessLogQueryService archiveAccessLogQueryService;

    @GetMapping
    @Operation(summary = "查询档案访问日志（游标分页）")
    public R<CursorResult<ArchiveAccessLogResponse>> list(@Valid ArchiveAccessLogQuery query) {
        requireBackOrAdmin();
        return R.ok(archiveAccessLogQueryService.query(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出档案访问日志(xlsx)")
    public void export(@Valid ArchiveAccessLogQuery query, HttpServletResponse response) throws IOException {
        requireBackOrAdmin();
        List<ArchiveAccessLogResponse> rows = archiveAccessLogQueryService.listForExport(query);
        String[] headers = {"ID", "访问时间", "用户类型", "用户ID", "档案ID", "档案文件ID", "访问类型", "IP"};
        List<String[]> data = rows.stream().map(l -> new String[]{
                String.valueOf(l.getId()),
                l.getAccessedAt() != null ? l.getAccessedAt().toString() : "",
                l.getUserType() != null ? l.getUserType() : "",
                l.getUserId() != null ? String.valueOf(l.getUserId()) : "",
                l.getArchiveId() != null ? String.valueOf(l.getArchiveId()) : "",
                l.getArchiveFileId() != null ? String.valueOf(l.getArchiveFileId()) : "",
                l.getAccessType() != null ? l.getAccessType() : "",
                l.getIpAddress() != null ? l.getIpAddress() : ""
        }).toList();
        LogExcelExporter.write(response, "archive-access-logs.xlsx", headers, data);
    }

    private void requireBackOrAdmin() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
