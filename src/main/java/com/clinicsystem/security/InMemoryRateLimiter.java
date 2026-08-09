package com.clinicsystem.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-instance fixed-window rate limiter. State lives in a local map, so
 * it only works within one JVM. Use {@link RedisRateLimiter} when the app
 * runs behind a load balancer with multiple replicas.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private static final class Bucket {
        long windowStartMillis;
        int count;
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        long windowMillis = windowSeconds * 1000;
        long now = System.currentTimeMillis();

        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMillis > windowMillis) {
                Bucket fresh = new Bucket();
                fresh.windowStartMillis = now;
                fresh.count = 0;
                return fresh;
            }
            return existing;
        });

        synchronized (bucket) {
            if (bucket.count >= maxRequests) {
                return false;
            }
            bucket.count++;
            return true;
        }
    }
}
