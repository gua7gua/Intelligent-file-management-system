package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.CursorResult;
import com.archive.common.ErrorCode;
import com.archive.common.R;
import com.archive.dto.request.AuditLogQuery;
import com.archive.dto.response.AuditLogResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.AuditLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "操作审计日志查询（系统管理员，只读）；导出由前端 CSV 完成")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @Operation(summary = "查询审计日志（游标分页）")
    public R<CursorResult<AuditLogResponse>> list(@Valid AuditLogQuery query) {
        requireSysAdmin();
        return R.ok(auditLogQueryService.query(query));
    }

    private void requireSysAdmin() {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可查询审计日志");
        }
    }
}
