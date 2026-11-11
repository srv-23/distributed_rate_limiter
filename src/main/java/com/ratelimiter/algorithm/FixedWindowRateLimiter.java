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

    public FixedWindowRateLimiter(StringRedisTemplate redisTemplate, RateLimiterScripts scripts) {
        this.redisTemplate = redisTemplate;
        this.scripts = scripts;
    }

    public RateLimitResult allowRequest(String userId) {
        String key = "rate_limit:user:" + userId;

        List<Long> result = redisTemplate.execute(
                scripts.getFixedWindowScript(),
                List.of(key),
                String.valueOf(MAX_REQUESTS),
                String.valueOf(WINDOW_SECONDS)
        );

        if(result == null || result.size() != 3){
            throw new IllegalStateException("Invalid response from Redis script");
        }

        return new RateLimitResult(
                result.get(0) == 1,
                result.get(1),
                result.get(2)
        );
    }
}
