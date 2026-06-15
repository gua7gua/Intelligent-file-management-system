package com.archive.service;

/**
 * 短信验证码服务。验证码的生成/存储/过期/一次性校验由实现方托管
 * （阿里云实现托管于阿里云；fallback 实现为固定码）。
 */
public interface SmsCodeService {
    /** 发送验证码 */
    void send(String phone, String scene);

    /** 校验验证码（一次性） */
    boolean verify(String phone, String code);
}
