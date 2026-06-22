package com.archive.config;

import com.archive.common.ErrorCode;
import com.archive.entity.SystemConfig;
import com.archive.exception.BusinessException;
import com.archive.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 短信验证码内存限流：同手机号冷却（默认 60s，可经 system_configs 的 sms.code_cooldown_seconds 配置）；同 IP 每小时 10 次。
 * 单实例方案，多实例部署需换 Redis。
 */
@Component
public class SmsRateLimiter {

    /** 配置键：同一手机号两次发码的最小间隔（秒）。 */
    private static final String CONFIG_KEY = "sms.code_cooldown_seconds";
    /** 冷却配置兜底默认值（配置缺失/读库失败时使用）。 */
    private static final long DEFAULT_PHONE_COOLDOWN_MS = 60_000L;
    /** 冷却配置读库结果缓存时长，避免每次发码都查 DB。 */
    private static final long CONFIG_TTL_MS = 30_000L;
    private static final long IP_WINDOW_MS = 60 * 60 * 1000L;
    private static final int IP_HOURLY_LIMIT = 10;

    private final LongSupplier clock;
    private final SystemConfigMapper configMapper;
    private final Map<String, Long> phoneLastSend = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> ipSendTimes = new ConcurrentHashMap<>();

    /** volatile：发码是多线程并发场景，冷却值与缓存到期点需对其它线程可见。 */
    private volatile long cachedCooldownMs = DEFAULT_PHONE_COOLDOWN_MS;
    private volatile long cacheExpireAt = 0L;

    @Autowired
    public SmsRateLimiter(SystemConfigMapper configMapper) {
        this(System::currentTimeMillis, configMapper);
    }

    public SmsRateLimiter(LongSupplier clock) {
        this(clock, null);
    }

    public SmsRateLimiter(LongSupplier clock, SystemConfigMapper configMapper) {
        this.clock = clock;
        this.configMapper = configMapper;
    }

    /** 校验并记录一次发送；超限抛 TOO_MANY_REQUESTS */
    public void acquire(String phone, String ip) {
        long now = clock.getAsLong();
        long cooldownMs = phoneCooldownMs(now);

        Long last = phoneLastSend.get(phone);
        if (last != null && now - last < cooldownMs) {
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

    /**
     * 取同手机号冷却毫秒数。无 configMapper（单测）直接用默认值；否则按 TTL 缓存读 system_configs，
     * 读库失败兜底沿用上次值（首次失败用默认值），不阻断发码。
     */
    private long phoneCooldownMs(long now) {
        if (configMapper == null) {
            return DEFAULT_PHONE_COOLDOWN_MS;
        }
        if (now < cacheExpireAt) {
            return cachedCooldownMs;
        }
        try {
            SystemConfig cfg = configMapper.selectOne(
                    new QueryWrapper<SystemConfig>().eq("config_key", CONFIG_KEY));
            if (cfg != null && cfg.getConfigValue() != null) {
                long secs = Long.parseLong(cfg.getConfigValue().trim());
                // 负数或解析异常都兜底为上次/默认值；0 表示无冷却（合法）
                cachedCooldownMs = Math.max(0L, secs) * 1000L;
            }
        } catch (Exception e) {
            // 读库失败：沿用 cachedCooldownMs（首次即默认值），仅刷新到期点避免每次失败都查库
        }
        cacheExpireAt = now + CONFIG_TTL_MS;
        return cachedCooldownMs;
    }
}
