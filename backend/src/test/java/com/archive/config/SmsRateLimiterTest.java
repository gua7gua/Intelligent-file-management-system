package com.archive.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsRateLimiterTest {

    private final AtomicLong clock = new AtomicLong(1_000_000L);

    private SmsRateLimiter newLimiter() {
        return new SmsRateLimiter(() -> clock.get());
    }

    @Test
    void 同手机号60秒内第二次发码被拒() {
        SmsRateLimiter l = newLimiter();
        l.acquire("13800000005", "1.1.1.1");
        assertThatThrownBy(() -> l.acquire("13800000005", "1.1.1.1"))
                .hasMessageContaining("频繁");
    }

    @Test
    void 超过60秒可再次发码() {
        SmsRateLimiter l = newLimiter();
        l.acquire("13800000005", "1.1.1.1");
        clock.addAndGet(61_000L);
        assertThatCode(() -> l.acquire("13800000005", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void 同IP每小时超过10次被拒() {
        SmsRateLimiter l = newLimiter();
        for (int i = 0; i < 10; i++) {
            l.acquire("1380000000" + i, "9.9.9.9"); // 不同手机号，同 IP
        }
        assertThatThrownBy(() -> l.acquire("1380000009", "9.9.9.9"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void 超过1小时IP计数窗口滑动() {
        SmsRateLimiter l = newLimiter();
        for (int i = 0; i < 10; i++) {
            l.acquire("1380000000" + i, "9.9.9.9");
        }
        clock.addAndGet(60 * 60 * 1000L + 1); // 1 小时 + 1ms
        assertThatCode(() -> l.acquire("1380000009", "9.9.9.9")).doesNotThrowAnyException();
    }
}
