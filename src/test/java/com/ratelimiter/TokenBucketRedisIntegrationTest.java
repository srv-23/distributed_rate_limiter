
package com.ratelimiter;

import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.model.RateLimitPolicy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class TokenBucketRedisIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {

        registry.add("spring.data.redis.host", redis::getHost);

        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379)
        );
    }

    @Autowired
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @Test
    void shouldEnforceTokenBucketUsingRealRedis() {

        String userId = "testcontainers-" + UUID.randomUUID();
        String policyName = "integration-test";

        RateLimitPolicy policy = new RateLimitPolicy(
                10,
                0.01,
                1
        );

        int allowedRequests = 0;
        int rejectedRequests = 0;

        for (int i = 0; i < 11; i++) {

            var result = tokenBucketRateLimiter.allowRequest(
                    userId,
                    policyName,
                    policy
            );

            if (result.isAllowed()) {
                allowedRequests++;
            } else {
                rejectedRequests++;
            }
        }

        assertEquals(10, allowedRequests);
        assertEquals(1, rejectedRequests);
    }

    @Test
    void shouldEnforceTokenBucketUnderConcurrentRequests() throws Exception {

        String userId = "concurrent-test-" + UUID.randomUUID();
        String policyName = "integration-test";

        RateLimitPolicy policy = new RateLimitPolicy(
                10,
                0.01,
                1
        );

        int totalRequests = 100;
        int threadCount = 20;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {

            for (int i = 0; i < totalRequests; i++) {

                Future<Boolean> future = executor.submit(() -> {

                    ready.countDown();

                    start.await();

                    var result = tokenBucketRateLimiter.allowRequest(
                            userId,
                            policyName,
                            policy
                    );

                    return result.isAllowed();
                });

                futures.add(future);
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Worker threads did not become ready in time"
            );

            // Release all 20 worker threads together.
            start.countDown();

            int allowedRequests = 0;

            for (Future<Boolean> future : futures) {

                if (future.get()) {
                    allowedRequests++;
                }
            }

            int rejectedRequests = totalRequests - allowedRequests;

            System.out.println("Total requests: " + totalRequests);
            System.out.println("Allowed requests: " + allowedRequests);
            System.out.println("Rejected requests: " + rejectedRequests);

            assertEquals(10, allowedRequests);
            assertEquals(90, rejectedRequests);

        } finally {

            start.countDown();
            executor.shutdownNow();
        }
    }
}
