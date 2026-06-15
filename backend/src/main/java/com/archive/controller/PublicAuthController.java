package com.archive.controller;

import com.archive.common.R;
import com.archive.config.SmsRateLimiter;
import com.archive.dto.request.ResetPasswordRequest;
import com.archive.dto.request.SmsCodeRequest;
import com.archive.dto.response.SmsCodeResponse;
import com.archive.service.PublicPasswordService;
import com.archive.service.SmsCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
@Tag(name = "公众认证", description = "发送验证码、找回密码（匿名）")
public class PublicAuthController {

    private final SmsCodeService smsCodeService;
    private final PublicPasswordService publicPasswordService;
    private final SmsRateLimiter rateLimiter;

    @Value("${aliyun.sms-auth.enabled:false}")
    private boolean aliyunEnabled;

    @PostMapping("/sms-code")
    @Operation(summary = "发送短信验证码")
    public R<SmsCodeResponse> smsCode(@Valid @RequestBody SmsCodeRequest req, HttpServletRequest http) {
        rateLimiter.acquire(req.getPhone(), clientIp(http));
        smsCodeService.send(req.getPhone(), req.getScene());
        String msg = aliyunEnabled ? "验证码已发送" : "验证码已发送（fallback 固定码 123456）";
        return R.ok(SmsCodeResponse.of(!aliyunEnabled, msg));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "公众找回密码")
    public R<Boolean> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        publicPasswordService.resetPassword(req);
        return R.ok(true);
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
