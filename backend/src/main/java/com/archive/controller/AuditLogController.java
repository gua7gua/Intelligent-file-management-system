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
import com.archive.util.LogExcelExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "操作审计日志查询/导出（系统管理员，只读）")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "查询审计日志（游标分页）")
    public R<CursorResult<AuditLogResponse>> list(@Valid AuditLogQuery query) {
        requireSysAdmin();
        return R.ok(auditLogQueryService.query(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出审计日志(xlsx)")
    public void export(@Valid AuditLogQuery query, HttpServletResponse response) throws IOException {
        requireSysAdmin();
        List<AuditLogResponse> rows = auditLogQueryService.listForExport(query);
        String[] headers = {"ID", "操作时间", "操作人类型", "操作人ID", "模块", "操作类型", "业务类型", "业务ID", "详情", "IP"};
        List<String[]> data = rows.stream().map(l -> new String[]{
                String.valueOf(l.getId()),
                l.getOperatedAt() != null ? l.getOperatedAt().toString() : "",
                l.getActorType() != null ? l.getActorType() : "",
                l.getActorUserId() != null ? String.valueOf(l.getActorUserId()) : "",
                l.getModuleName() != null ? l.getModuleName() : "",
                l.getOperationType() != null ? l.getOperationType() : "",
                l.getBusinessType() != null ? l.getBusinessType() : "",
                l.getBusinessId() != null ? String.valueOf(l.getBusinessId()) : "",
                detailToJson(l),
                l.getIpAddress() != null ? l.getIpAddress() : ""
        }).toList();
        LogExcelExporter.write(response, "audit-logs.xlsx", headers, data);
    }

    private String detailToJson(AuditLogResponse l) {
        try {
            return l.getDetail() != null ? objectMapper.writeValueAsString(l.getDetail()) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void requireSysAdmin() {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可查询审计日志");
        }
    }
}
