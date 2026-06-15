package com.archive.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 配置缺失（aliyun.sms-auth.enabled=false）时的兜底实现：固定验证码 123456 + 日志输出。
 * 仅用于本地/CI 联调，不可用于生产。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "aliyun.sms-auth", name = "enabled", havingValue = "false", matchIfMissing = true)
public class FallbackSmsCodeService implements SmsCodeService {

    private static final String FALLBACK_CODE = "123456";

    @Override
    public void send(String phone, String scene) {
        log.warn("[SMS-FALLBACK] phone={} scene={} 固定验证码={}", phone, scene, FALLBACK_CODE);
    }

    @Override
    public boolean verify(String phone, String code) {
        return FALLBACK_CODE.equals(code);
    }
}
