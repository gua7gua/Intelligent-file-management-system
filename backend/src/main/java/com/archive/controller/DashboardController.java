package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.R;
import com.archive.dto.response.AdminDashboardResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台概览（§6.1）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "管理概览", description = "管理后台概览聚合统计")
public class DashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/api/admin/dashboard")
    @Operation(summary = "6.1 管理概览统计")
    public R<AdminDashboardResponse> overview() {
        if (!AuthContext.hasRole(RoleCode.front_archivist)
                && !AuthContext.hasRole(RoleCode.back_archivist)
                && !AuthContext.hasRole(RoleCode.director)
                && !AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
        return R.ok(adminDashboardService.overview());
    }
}
