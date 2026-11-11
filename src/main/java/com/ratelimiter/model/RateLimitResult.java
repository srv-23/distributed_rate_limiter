package com.ratelimiter.model;

public class RateLimitResult {
    private final boolean allowed;
    private final long currentCount;
    private final long remainingRequests;

    public RateLimitResult(boolean allowed, long currentCount, long remainingRequests) {
        this.allowed = allowed;
        this.currentCount = currentCount;
        this.remainingRequests = remainingRequests;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public long getRemainingRequests() {
        return remainingRequests;
    }
}
