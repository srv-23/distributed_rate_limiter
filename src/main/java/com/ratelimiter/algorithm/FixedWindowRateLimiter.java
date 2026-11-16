package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.redis.RateLimiterScripts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FixedWindowRateLimiter {

    private static final long MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterScripts scripts;

    public FixedWindowRateLimiter(
            StringRedisTemplate redisTemplate,
            RateLimiterScripts scripts
    ) {
        this.redisTemplate = redisTemplate;
        this.scripts = scripts;
    }

    public RateLimitResult allowRequest(String userId) {

        String key = "rate_limit:user:" + userId;

        List<?> result = redisTemplate.execute(
                scripts.getFixedWindowScript(),
                List.of(key),
                String.valueOf(MAX_REQUESTS),
                String.valueOf(WINDOW_SECONDS)
        );

        if (result == null || result.size() != 3) {
            throw new IllegalStateException(
                    "Invalid response from Redis script"
            );
        }

        if (!(result.get(0) instanceof Number allowed)
                || !(result.get(1) instanceof Number currentCount)
                || !(result.get(2) instanceof Number remaining)) {

            throw new IllegalStateException(
                    "Redis script returned unexpected result types"
            );
        }

        return new RateLimitResult(
                allowed.longValue() == 1,
                currentCount.longValue(),
                remaining.longValue()
        );
    }
}
