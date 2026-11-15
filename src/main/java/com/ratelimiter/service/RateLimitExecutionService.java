package com.ratelimiter.service;

import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.config.RateLimitProperties;
import com.ratelimiter.model.RateLimitDecision;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.TokenBucketResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class RateLimitExecutionService {
    public static final Logger logger = LoggerFactory.getLogger(RateLimitExecutionService.class);
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitExecutionService(TokenBucketRateLimiter tokenBucketRateLimiter, RateLimitProperties rateLimitProperties) {
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    public RateLimitDecision execute(
            String userId,
            String policyName,
            RateLimitPolicy policy
    ) {
        try{
            TokenBucketResult result = tokenBucketRateLimiter.allowRequest(userId, policyName, policy);

            return new RateLimitDecision(
                    result.isAllowed(),
                    true,
                    result.getRemainingTokens(),
                    result.getRetryAfterSeconds()
            );
        } catch (DataAccessException exception){
            if("fail-open".equals(rateLimitProperties.failureStrategy())){
                logger.warn("Redis unavailable. Allowing request because fail-open is configured. User: {}, Policy: {}", userId, policyName, exception);
                return new RateLimitDecision(
                        true,
                        false,
                        null,
                        0
                );
            }
            throw exception;
        }
    }
}
