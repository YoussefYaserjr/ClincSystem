package com.clinicsystem.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Distributed fixed-window rate limiter backed by Redis.
 *
 * <p>Every replica shares the same Redis, so the counter for a user is
 * global across the whole cluster. The {@code INCR} + {@code EXPIRE} check is
 * wrapped in a single Lua script, which Redis executes atomically — that is
 * what makes this correct under concurrency where the in-memory variant is not.
 *
 * <p>If Redis is unreachable the limiter <em>fails open</em> (allows the
 * request) with a throttled warning, so a broken cache never takes down the API.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String KEY_PREFIX = "rate-limit:";

    /**
     * Atomic fixed-window: increment the counter, set the TTL on first hit,
     * then reject if the window limit is exceeded.
     * KEYS[1] = bucket key, ARGV[1] = max requests, ARGV[2] = window (seconds).
     * Returns 1 (allow) or 0 (reject).
     */
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                return 0
            end
            return 1
            """, Long.class);

    private static final long LOG_THROTTLE_MILLIS = 30_000;

    private final StringRedisTemplate redisTemplate;
    private volatile long lastErrorLogMillis;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        try {
            Long result = redisTemplate.execute(FIXED_WINDOW_SCRIPT,
                    List.of(KEY_PREFIX + key),
                    String.valueOf(maxRequests), String.valueOf(windowSeconds));
            return result == null || result == 1L;
        } catch (RuntimeException e) {
            warnThrottled("Redis unavailable ({}), failing open for rate limit key '{}'", e.getMessage(), key);
            return true;
        }
    }

    private void warnThrottled(String format, Object... args) {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (now - lastErrorLogMillis > LOG_THROTTLE_MILLIS) {
                lastErrorLogMillis = now;
                log.warn(format, args);
            }
        }
    }
}
