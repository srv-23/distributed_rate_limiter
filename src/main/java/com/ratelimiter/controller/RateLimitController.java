package com.ratelimiter.controller;

import com.ratelimiter.annotation.RateLimited;
import com.ratelimiter.algorithm.FixedWindowRateLimiter;
import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.config.RateLimitProperties;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.TokenBucketResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class RateLimitController {
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitController(FixedWindowRateLimiter fixedWindowRateLimiter, TokenBucketRateLimiter tokenBucketRateLimiter, RateLimitProperties rateLimitProperties) {
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @GetMapping("/test-limit")
    public RateLimitResult testFixedWindow(@RequestParam String userId) {
        return fixedWindowRateLimiter.allowRequest(userId);
    }

    @GetMapping("/test-token-bucket")
    public TokenBucketResult testTokenBucket(
            @RequestParam String userId,
            @RequestParam(defaultValue = "profile") String policyName
    ) {

        RateLimitProperties.Policy configuredPolicy =
                rateLimitProperties.policies().get(policyName);

        if (configuredPolicy == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown rate limit policy: " + policyName
            );
        }

        RateLimitPolicy policy = new RateLimitPolicy(
                configuredPolicy.capacity(),
                configuredPolicy.refillRate(),
                configuredPolicy.requestCost()
        );

        return tokenBucketRateLimiter.allowRequest(
                userId,
                policyName,
                policy
        );
    }

    @GetMapping("/interceptor/profile")
    @RateLimited(policy = "profile")
    public String testInterceptor() {
        return "Request successfully passed through RateLimitInterceptor!";
    }

}