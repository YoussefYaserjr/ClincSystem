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

/**
 * Enforces a fixed-window limit on authentication and appointment-booking
 * endpoints. The actual counting is delegated to a {@link RateLimiter}, so the
 * same filter works for a single instance (in-memory) or a replicated
 * deployment behind a load balancer (Redis). Disabled by default
 * (see {@code app.rate-limit.*}).
 */
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private static final int TOO_MANY_REQUESTS = 429;

    private record Rule(String name, int max, long windowSeconds) {}

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

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

        if (!rateLimiter.tryAcquire(key, rule.max(), rule.windowSeconds())) {
            response.setStatus(TOO_MANY_REQUESTS);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiError.builder()
                    .status(TOO_MANY_REQUESTS)
                    .message("Too many requests, please slow down")
                    .timestamp(LocalDateTime.now())
                    .build());
            return;
        }

        chain.doFilter(request, response);
    }

    private Rule match(String method, String path) {
        if (path.startsWith("/auth/")) {
            return new Rule("auth", properties.getAuthMax(), properties.getAuthWindowSeconds());
        }
        if ("POST".equals(method) && "/appointments".equals(path)) {
            return new Rule("booking", properties.getBookingMax(), properties.getBookingWindowSeconds());
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
