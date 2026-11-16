
package com.ratelimiter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitPolicyTest {

    @Test
    void shouldAcceptValidPolicy() {
        assertDoesNotThrow(() ->
                new RateLimitPolicy(10, 2, 1)
        );
    }

    @Test
    void shouldRejectZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(0, 2, 1)
        );
    }

    @Test
    void shouldRejectNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(-10, 2, 1)
        );
    }

    @Test
    void shouldRejectInvalidRefillRate() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, 0, 1)
        );
    }

    @Test
    void shouldRejectInvalidRequestCost() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, 2, 0)
        );
    }

    @Test
    void shouldRejectRequestCostGreaterThanCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, 2, 11)
        );
    }

    @Test
    void shouldRejectNaNValues() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(Double.NaN, 2, 1)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, Double.NaN, 1)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, 2, Double.NaN)
        );
    }

    @Test
    void shouldRejectInfiniteValues() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(Double.POSITIVE_INFINITY, 2, 1)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, Double.POSITIVE_INFINITY, 1)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(10, 2, Double.POSITIVE_INFINITY)
        );
    }
}
