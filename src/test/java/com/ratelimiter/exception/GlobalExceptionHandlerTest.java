
package com.ratelimiter.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturn503WhenRedisFails() {

        DataAccessResourceFailureException exception =
                new DataAccessResourceFailureException(
                        "Redis connection failed"
                );

        ResponseEntity<Map<String, String>> response =
                handler.handleRedisException(exception);

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Service Unavailable",
                response.getBody().get("error")
        );

        assertEquals(
                "Rate limiting service is temporarily unavailable.",
                response.getBody().get("message")
        );
    }
}
