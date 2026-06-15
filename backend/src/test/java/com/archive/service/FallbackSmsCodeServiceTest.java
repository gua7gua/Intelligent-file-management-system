package com.archive.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackSmsCodeServiceTest {

    @Test
    void fallback_固定码123456校验通过() {
        FallbackSmsCodeService svc = new FallbackSmsCodeService();
        svc.send("13800000005", "forgot_password"); // 仅日志，无异常即通过
        assertThat(svc.verify("13800000005", "123456")).isTrue();
        assertThat(svc.verify("13800000005", "000000")).isFalse();
    }
}
