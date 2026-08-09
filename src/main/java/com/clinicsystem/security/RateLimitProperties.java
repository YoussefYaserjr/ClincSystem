package com.clinicsystem.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled;
    private int authMax = 20;
    private long authWindowSeconds = 60;
    private int bookingMax = 10;
    private long bookingWindowSeconds = 60;
}
