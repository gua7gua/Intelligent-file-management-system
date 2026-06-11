package com.archive.controller;

import com.archive.common.R;
import com.archive.common.TraceFilter;
import com.archive.dto.request.LoginRequest;
import com.archive.dto.response.LoginResponse;
import com.archive.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "登录、登出、当前用户")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        LoginResponse resp = authService.login(req);
        return R.ok(resp).traceId(TraceFilter.getTraceId(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<LoginResponse.UserInfoResponse> me() {
        return R.ok(authService.me());
    }
}
