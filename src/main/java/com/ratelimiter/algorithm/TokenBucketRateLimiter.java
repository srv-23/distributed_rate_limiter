package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.TokenBucketResult;
import com.ratelimiter.redis.RateLimiterScripts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenBucketRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final RateLimiterScripts scripts;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate, RateLimiterScripts scripts) {
        this.redisTemplate = redisTemplate;
        this.scripts = scripts;
    }

    public TokenBucketResult allowRequest(String userId, RateLimitPolicy policy) {
        String key = "rate_limit:token_bucket:user:" + userId;
        List<?> result = redisTemplate.execute(
                scripts.getTokenBucketScript(),
                List.of(key),
                String.valueOf(policy.bucketCapacity()),
                String.valueOf(policy.refillRate()),
                String.valueOf(policy.requestCost())
        );

        if(result == null || result.size() != 2){
            throw new IllegalStateException("Invalid response from Redis Token Bucket script");
        }
        boolean allowed = "1".equals(result.get(0).toString());
        double remainingTokens = Double.parseDouble(result.get(1).toString());
        return new TokenBucketResult(allowed, remainingTokens);
    }
}
