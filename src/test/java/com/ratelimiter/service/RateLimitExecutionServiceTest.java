
package com.ratelimiter.service;

import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.config.RateLimitProperties;
import com.ratelimiter.model.RateLimitDecision;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.TokenBucketResult;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitExecutionServiceTest {

    private final TokenBucketRateLimiter tokenBucketRateLimiter =
            mock(TokenBucketRateLimiter.class);

    private RateLimitExecutionService createService(String strategy) {
        RateLimitProperties properties =
                new RateLimitProperties(strategy, Map.of());

        return new RateLimitExecutionService(
                tokenBucketRateLimiter,
                properties
        );
    }

    @Test
    void shouldReturnEvaluatedDecisionWhenRedisWorks() {

        RateLimitExecutionService service =
                createService("fail-closed");

        RateLimitPolicy policy = new RateLimitPolicy(10, 2, 1);

        when(tokenBucketRateLimiter.allowRequest("101", "profile", policy))
                .thenReturn(new TokenBucketResult(true, 9, 0));

        RateLimitDecision decision =
                service.execute("101", "profile", policy);

        assertTrue(decision.allowed());
        assertTrue(decision.rateLimitEvaluated());
        assertEquals(9.0, decision.remainingTokens());
        assertEquals(0, decision.retryAfterSeconds());
    }

    @Test
    void shouldAllowRequestWhenRedisFailsAndStrategyIsFailOpen() {

        RateLimitExecutionService service =
                createService("fail-open");

        RateLimitPolicy policy = new RateLimitPolicy(10, 2, 1);

        when(tokenBucketRateLimiter.allowRequest("101", "profile", policy))
                .thenThrow(new DataAccessResourceFailureException(
                        "Redis unavailable"
                ));

        RateLimitDecision decision =
                service.execute("101", "profile", policy);

        assertTrue(decision.allowed());
        assertFalse(decision.rateLimitEvaluated());
        assertNull(decision.remainingTokens());
        assertEquals(0, decision.retryAfterSeconds());
    }

    @Test
    void shouldThrowExceptionWhenRedisFailsAndStrategyIsFailClosed() {

        RateLimitExecutionService service =
                createService("fail-closed");

        RateLimitPolicy policy = new RateLimitPolicy(10, 2, 1);

        when(tokenBucketRateLimiter.allowRequest("101", "profile", policy))
                .thenThrow(new DataAccessResourceFailureException(
                        "Redis unavailable"
                ));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.execute("101", "profile", policy)
        );
    }
}
