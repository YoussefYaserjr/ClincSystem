package com.clinicsystem.security;

import com.clinicsystem.exception.ApiError;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory fixed-window rate limiter applied to authentication and
 * appointment-booking endpoints. Disabled by default (see app.rate-limit.*).
 */
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private static final int TOO_MANY_REQUESTS = 429;

    private static final class Bucket {
        long windowStartMillis;
        int count;
    }

    private record Rule(String name, int max, long windowMillis) {}

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = match(request.getMethod(), request.getRequestURI());
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + "|" + rule.name();
        long now = System.currentTimeMillis();

        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMillis > rule.windowMillis()) {
                Bucket fresh = new Bucket();
                fresh.windowStartMillis = now;
                fresh.count = 0;
                return fresh;
            }
            return existing;
        });

        synchronized (bucket) {
            if (bucket.count >= rule.max()) {
                response.setStatus(TOO_MANY_REQUESTS);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), ApiError.builder()
                        .status(TOO_MANY_REQUESTS)
                        .message("Too many requests, please slow down")
                        .timestamp(LocalDateTime.now())
                        .build());
                return;
            }
            bucket.count++;
        }

        chain.doFilter(request, response);
    }

    private Rule match(String method, String path) {
        if (path.startsWith("/auth/")) {
            return new Rule("auth", properties.getAuthMax(), properties.getAuthWindowSeconds() * 1000);
        }
        if ("POST".equals(method) && "/appointments".equals(path)) {
            return new Rule("booking", properties.getBookingMax(), properties.getBookingWindowSeconds() * 1000);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
