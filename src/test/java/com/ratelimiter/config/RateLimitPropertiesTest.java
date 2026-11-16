
package com.ratelimiter.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitPropertiesTest {

    @Test
    void shouldDefaultToFailClosedWhenStrategyIsNull() {
        RateLimitProperties properties =
                new RateLimitProperties(null, Map.of());

        assertEquals("fail-closed", properties.failureStrategy());
    }

    @Test
    void shouldDefaultToFailClosedWhenStrategyIsBlank() {
        RateLimitProperties properties =
                new RateLimitProperties("   ", Map.of());

        assertEquals("fail-closed", properties.failureStrategy());
    }

    @Test
    void shouldAcceptFailOpenStrategy() {
        RateLimitProperties properties =
                new RateLimitProperties("fail-open", Map.of());

        assertEquals("fail-open", properties.failureStrategy());
    }

    @Test
    void shouldAcceptFailClosedStrategy() {
        RateLimitProperties properties =
                new RateLimitProperties("fail-closed", Map.of());

        assertEquals("fail-closed", properties.failureStrategy());
    }

    @Test
    void shouldIgnoreStrategyCase() {
        RateLimitProperties properties =
                new RateLimitProperties("FAIL-OPEN", Map.of());

        assertEquals("fail-open", properties.failureStrategy());
    }

    @Test
    void shouldRejectInvalidStrategy() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitProperties("invalid", Map.of())
        );
    }
}
