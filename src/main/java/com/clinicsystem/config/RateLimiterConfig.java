package com.clinicsystem.config;

import com.clinicsystem.security.InMemoryRateLimiter;
import com.clinicsystem.security.RateLimiter;
import com.clinicsystem.security.RateLimitProperties;
import com.clinicsystem.security.RedisRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Selects the rate-limit store. {@code app.rate-limit.store=redis} uses the
 * distributed {@link RedisRateLimiter} shared by every replica; anything else
 * uses the single-instance {@link InMemoryRateLimiter}.
 */
@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    public RateLimiter rateLimiter(RateLimitProperties properties,
                                   ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        if ("redis".equalsIgnoreCase(properties.getStore())) {
            StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
            if (template != null) {
                return new RedisRateLimiter(template);
            }
            log.warn("app.rate-limit.store=redis but no Redis connection configured; falling back to in-memory rate limiting");
        }
        return new InMemoryRateLimiter();
    }
}
