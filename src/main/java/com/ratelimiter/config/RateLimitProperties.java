
package com.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limiter")
public record RateLimitProperties(
        String failureStrategy,
        Map<String, Policy> policies
) {

    public RateLimitProperties {
        if (failureStrategy == null || failureStrategy.isBlank()) {
            failureStrategy = "fail-closed";
        } else {
            failureStrategy = failureStrategy.toLowerCase(Locale.ROOT);
        }

        if (!failureStrategy.equals("fail-open")
                && !failureStrategy.equals("fail-closed")) {
            throw new IllegalArgumentException(
                    "Invalid failure strategy: " + failureStrategy
            );
        }
    }

    public record Policy(
            double capacity,
            double refillRate,
            double requestCost
    ) {
    }
}
