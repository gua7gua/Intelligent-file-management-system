package com.archive.controller;

import com.archive.common.PageRequest;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户增删改查（管理员）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "用户列表（分页）")
    public R<PageResult<UserInfoResponse>> list(@Valid PageRequest page,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status) {
        return R.ok(userService.listUsers(page.getPageNo(), page.getPageSize(), keyword, status));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public R<UserInfoResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return R.ok(userService.createUser(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑用户")
    public R<UserInfoResponse> update(@PathVariable Long id,
                                      @Valid @RequestBody UserUpdateRequest req) {
        return R.ok(userService.updateUser(id, req));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码")
    public R<Void> resetPassword(@PathVariable Long id,
                                  @RequestBody(required = false) Map<String, String> body) {
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.resetPassword(id, newPassword);
        return R.ok();
    }
}
