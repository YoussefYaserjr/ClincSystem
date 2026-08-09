package com.clinicsystem.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter();

    @Test
    void allowsUpToMaxRequestsPerWindow() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("key", 5, 60)).isTrue();
        }
        assertThat(limiter.tryAcquire("key", 5, 60)).isFalse();
    }

    @Test
    void keysAreIsolated() {
        assertThat(limiter.tryAcquire("client-a", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("client-b", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("client-a", 1, 60)).isFalse();
        assertThat(limiter.tryAcquire("client-b", 1, 60)).isFalse();
    }
}
