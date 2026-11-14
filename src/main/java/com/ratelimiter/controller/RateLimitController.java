package com.ratelimiter.controller;

import com.ratelimiter.algorithm.FixedWindowRateLimiter;
import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.TokenBucketResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;

    public RateLimitController(FixedWindowRateLimiter fixedWindowRateLimiter, TokenBucketRateLimiter tokenBucketRateLimiter) {
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
    }

    @GetMapping("/test-limit")
    public RateLimitResult testFixedWindow(@RequestParam String userId) {
        return fixedWindowRateLimiter.allowRequest(userId);
    }

    @GetMapping("/test-token-bucket")
    public TokenBucketResult testTokenBucket(@RequestParam String userId) {
        RateLimitPolicy policy = new RateLimitPolicy(10, 2, 1);
        return tokenBucketRateLimiter.allowRequest(userId, policy);
    }
}
