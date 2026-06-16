package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.OrganizationCreateRequest;
import com.archive.dto.request.OrganizationQuery;
import com.archive.dto.request.OrganizationUpdateRequest;
import com.archive.dto.response.OrganizationResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@Tag(name = "组织维护", description = "组织查询/新增（并入用户管理页）")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "查询组织列表")
    public R<PageResult<OrganizationResponse>> list(@Valid OrganizationQuery query) {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.sys_admin))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足");
        }
        return R.ok(organizationService.listOrganizations(query));
    }

    @PostMapping
    @Operation(summary = "新增组织（仅系统管理员）")
    public R<OrganizationResponse> create(@Valid @RequestBody OrganizationCreateRequest req) {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可新增组织");
        }
        return R.ok(organizationService.createOrganization(req));
    }

    @PutMapping("/{organizationId}")
    @Operation(summary = "更新组织（仅系统管理员；支持停用）")
    public R<OrganizationResponse> update(@PathVariable Long organizationId,
                                          @Valid @RequestBody OrganizationUpdateRequest req) {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可更新组织");
        }
        return R.ok(organizationService.updateOrganization(organizationId, req));
    }

    @DeleteMapping("/{organizationId}")
    @Operation(summary = "删除组织（仅系统管理员；无关联全宗和用户时方可删除，否则请改用停用）")
    public R<Void> delete(@PathVariable Long organizationId) {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可删除组织");
        }
        organizationService.deleteOrganization(organizationId);
        return R.ok();
    }
}
