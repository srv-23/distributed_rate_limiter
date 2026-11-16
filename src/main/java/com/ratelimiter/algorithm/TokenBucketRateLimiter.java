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

    public TokenBucketRateLimiter(
            StringRedisTemplate redisTemplate,
            RateLimiterScripts scripts
    ) {
        this.redisTemplate = redisTemplate;
        this.scripts = scripts;
    }

    public TokenBucketResult allowRequest(
            String userId,
            String policyName,
            RateLimitPolicy policy
    ) {

        String key = "rate_limit:token_bucket:user:"
                + policyName + ":user:" + userId;

        List<?> result = redisTemplate.execute(
                scripts.getTokenBucketScript(),
                List.of(key),
                String.valueOf(policy.bucketCapacity()),
                String.valueOf(policy.refillRate()),
                String.valueOf(policy.requestCost())
        );

        if (result == null || result.size() != 3) {
            throw new IllegalStateException(
                    "Invalid response from Redis Token Bucket script"
            );
        }

        try {
            String allowedValue = result.get(0).toString();
            String remainingValue = result.get(1).toString();
            String retryAfterValue = result.get(2).toString();

            if (!allowedValue.equals("0") && !allowedValue.equals("1")) {
                throw new IllegalStateException(
                        "Invalid allowed value from Redis Token Bucket script"
                );
            }

            double remainingTokens = Double.parseDouble(remainingValue);
            long retryAfterSeconds = Long.parseLong(retryAfterValue);

            if (!Double.isFinite(remainingTokens)
                    || remainingTokens < 0
                    || retryAfterSeconds < 0) {
                throw new IllegalStateException(
                        "Invalid numeric values from Redis Token Bucket script"
                );
            }

            return new TokenBucketResult(
                    "1".equals(allowedValue),
                    remainingTokens,
                    retryAfterSeconds
            );

        } catch (NumberFormatException | NullPointerException exception) {
            throw new IllegalStateException(
                    "Unable to parse Redis Token Bucket script response",
                    exception
            );
        }
    }
}
