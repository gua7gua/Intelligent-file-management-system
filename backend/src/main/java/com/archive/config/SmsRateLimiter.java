package com.archive.config;

import com.archive.common.ErrorCode;
import com.archive.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 短信验证码内存限流：同手机号 60s 冷却；同 IP 每小时 10 次。
 * 单实例方案，多实例部署需换 Redis。
 */
@Component
public class SmsRateLimiter {

    private static final long PHONE_COOLDOWN_MS = 60_000L;
    private static final long IP_WINDOW_MS = 60 * 60 * 1000L;
    private static final int IP_HOURLY_LIMIT = 10;

    private final LongSupplier clock;
    private final Map<String, Long> phoneLastSend = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> ipSendTimes = new ConcurrentHashMap<>();

    public SmsRateLimiter() {
        this(System::currentTimeMillis);
    }

    public SmsRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /** 校验并记录一次发送；超限抛 TOO_MANY_REQUESTS */
    public void acquire(String phone, String ip) {
        long now = clock.getAsLong();

        Long last = phoneLastSend.get(phone);
        if (last != null && now - last < PHONE_COOLDOWN_MS) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }

        List<Long> times = ipSendTimes.computeIfAbsent(ip, k -> new ArrayList<>());
        synchronized (times) {
            prune(times, now);
            if (times.size() >= IP_HOURLY_LIMIT) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "该IP请求过于频繁，请稍后再试");
            }
            times.add(now);
        }
        phoneLastSend.put(phone, now);
    }

    private void prune(List<Long> times, long now) {
        Iterator<Long> it = times.iterator();
        while (it.hasNext()) {
            if (now - it.next() > IP_WINDOW_MS) {
                it.remove();
            } else {
                break;
            }
        }
    }
}
