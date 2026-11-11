package com.ratelimiter;

import com.ratelimiter.algorithm.FixedWindowRateLimiter;
import com.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class FixedWindowConcurrencyTest {

    @Autowired
    private FixedWindowRateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldAllowOnlyFiveRequestsUnderConcurrency()
            throws Exception {

        String userId = "concurrency-test-user";

        redisTemplate.delete("rate_limit:user:" + userId);

        int totalRequests = 100;

        ExecutorService executor =
                Executors.newFixedThreadPool(totalRequests);

        CountDownLatch ready = new CountDownLatch(totalRequests);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<RateLimitResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < totalRequests; i++) {

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    start.await();

                    return rateLimiter.allowRequest(userId);
                }));
            }

            ready.await();
            start.countDown();

            int allowed = 0;
            int rejected = 0;

            for (Future<RateLimitResult> future : futures) {
                RateLimitResult result = future.get();

                if (result.isAllowed()) {
                    allowed++;
                } else {
                    rejected++;
                }
            }

            assertEquals(5, allowed);
            assertEquals(95, rejected);

        } finally {
            executor.shutdownNow();
        }
    }
}