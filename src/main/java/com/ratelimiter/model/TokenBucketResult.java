package com.ratelimiter.model;

public class TokenBucketResult {
    private final boolean allowed;
    private final double remainingTokens;

    public TokenBucketResult(boolean allowed, double remainingTokens) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public double getRemainingTokens() {
        return remainingTokens;
    }
}
