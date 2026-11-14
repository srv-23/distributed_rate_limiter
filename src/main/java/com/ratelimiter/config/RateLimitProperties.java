package com.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "rate-limiter")
public record RateLimitProperties(
        Map<String,Policy> policies
) {
    public record Policy(
            double capacity,
            double refillRate,
            double requestCost
    ){

    }
}

