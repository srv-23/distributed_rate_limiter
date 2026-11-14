package com.ratelimiter.model;

public record RateLimitPolicy(
        double bucketCapacity,
        double refillRate,
        double requestCost
){
    public RateLimitPolicy {
        if(bucketCapacity <= 0) {
            throw new IllegalArgumentException("Bucket capacity must be greater than 0");
        }
        if(refillRate <= 0) {
            throw new IllegalArgumentException("Refill rate must be greater than 0");
        }
        if(requestCost <= 0 || requestCost > bucketCapacity) {
            throw new IllegalArgumentException("Request cost must be positive and cannot exceed bucket capacity");
        }
    }
}


