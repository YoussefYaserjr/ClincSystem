package com.clinicsystem.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimiterTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

    @Test
    void allowsRequestWhenScriptReturnsOne() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        assertThat(limiter.tryAcquire("1.2.3.4|auth", 20, 60)).isTrue();
    }

    @Test
    void rejectsRequestWhenLimitExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);

        assertThat(limiter.tryAcquire("1.2.3.4|auth", 20, 60)).isFalse();
    }

    @Test
    void treatsNullResultAsAllowed() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(null);

        assertThat(limiter.tryAcquire("1.2.3.4|auth", 20, 60)).isTrue();
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("Connection refused"));

        assertThat(limiter.tryAcquire("1.2.3.4|auth", 20, 60)).isTrue();
    }

    @Test
    void passesLimitAndWindowAsIndividualScriptArgs() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        limiter.tryAcquire("1.2.3.4|auth", 20, 60);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), args.capture());
        assertThat(args.getValue()).containsExactly("20", "60");
    }

    @Test
    void prefixesKey() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        limiter.tryAcquire("1.2.3.4|auth", 20, 60);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertThat(keys.getValue()).containsExactly("rate-limit:1.2.3.4|auth");
    }
}
