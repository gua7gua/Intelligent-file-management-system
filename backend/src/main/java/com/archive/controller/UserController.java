package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageRequest;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserStatusRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户、角色、状态（系统管理员）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询用户列表")
    public R<PageResult<UserInfoResponse>> list(
            @Valid PageRequest page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Long organizationId) {
        return R.ok(userService.listUsers(page.getPageNo(), page.getPageSize(),
                keyword, status, userType, roleCode, organizationId));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public R<UserInfoResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return R.ok(userService.createUser(req));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情")
    public R<UserInfoResponse> detail(@PathVariable Long userId) {
        return R.ok(userService.getUserDetail(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新用户")
    public R<UserInfoResponse> update(@PathVariable Long userId,
                                      @Valid @RequestBody UserUpdateRequest req) {
        return R.ok(userService.updateUser(userId, req));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "启用或禁用用户")
    public R<UserInfoResponse> updateStatus(@PathVariable Long userId,
                                            @RequestBody @Valid UserStatusRequest req) {
        return R.ok(userService.updateStatus(userId, req));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "重置内部用户密码")
    public R<Void> resetPassword(@PathVariable Long userId,
                                 @RequestBody(required = false) java.util.Map<String, String> body) {
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.resetPassword(userId, newPassword);
        return R.ok();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "软删除用户")
    public R<Void> delete(@PathVariable Long userId) {
        if (!AuthContext.hasRole(RoleCode.sys_admin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅系统管理员可删除用户");
        }
        userService.softDelete(userId);
        return R.ok();
    }
}
