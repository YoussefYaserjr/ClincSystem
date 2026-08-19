package com.clinicsystem.security;

/**
 * Shared contract for fixed-window rate limiting. Implementations must be
 * safe for concurrent use.
 */
public interface RateLimiter {

    /**
     * Attempts to consume one request against a fixed window.
     *
     * @param key           bucket key (e.g. userId or client IP + rule name)
     * @param maxRequests   maximum requests allowed per window
     * @param windowSeconds length of the window in seconds
     * @return {@code true} if the request is allowed, {@code false} to reject with 429
     */
    boolean tryAcquire(String key, int maxRequests, long windowSeconds);
}
