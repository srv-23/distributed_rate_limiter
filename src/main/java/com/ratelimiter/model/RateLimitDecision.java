package com.ratelimiter.model;

public record RateLimitDecision (
    boolean allowed,
    boolean rateLimitEvaluated,
    Double remainingTokens,
    long retryAfterSeconds
){

}
