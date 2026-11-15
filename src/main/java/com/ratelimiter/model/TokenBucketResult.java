package com.ratelimiter.model;

public class TokenBucketResult {
    private final boolean allowed;
    private final double remainingTokens;
    private final long retryAfterSeconds;

    public TokenBucketResult(boolean allowed, double remainingTokens, long retryAfterSeconds) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public double getRemainingTokens() {
        return remainingTokens;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
