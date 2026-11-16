
package com.ratelimiter.model;

public record RateLimitPolicy(
        double bucketCapacity,
        double refillRate,
        double requestCost
) {
    public RateLimitPolicy {

        if (!Double.isFinite(bucketCapacity) || bucketCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Bucket capacity must be a finite number greater than 0"
            );
        }

        if (!Double.isFinite(refillRate) || refillRate <= 0) {
            throw new IllegalArgumentException(
                    "Refill rate must be a finite number greater than 0"
            );
        }

        if (!Double.isFinite(requestCost)
                || requestCost <= 0
                || requestCost > bucketCapacity) {
            throw new IllegalArgumentException(
                    "Request cost must be finite, positive, and cannot exceed bucket capacity"
            );
        }
    }
}
