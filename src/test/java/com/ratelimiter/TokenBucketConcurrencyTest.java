
package com.ratelimiter;

import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.TokenBucketResult;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TokenBucketConcurrencyTest {

    @Autowired
    private TokenBucketRateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldAllowOnlyTenRequestsUnderConcurrency()
            throws Exception {

        String userId = "token-bucket-concurrency-test";

        redisTemplate.delete(
                "rate_limit:token_bucket:user:" + userId
        );

        int totalRequests = 100;
        RateLimitPolicy policy = new RateLimitPolicy(10, 2, 1);


        ExecutorService executor =
                Executors.newFixedThreadPool(totalRequests);

        CountDownLatch ready = new CountDownLatch(totalRequests);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<TokenBucketResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < totalRequests; i++) {

                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    return rateLimiter.allowRequest(userId,"profile", policy);
                }));
            }

            ready.await();
            start.countDown();

            int allowed = 0;
            int rejected = 0;

            for (Future<TokenBucketResult> future : futures) {
                TokenBucketResult result = future.get();

                if (result.isAllowed()) {
                    allowed++;
                } else {
                    rejected++;
                }
            }

            assertEquals(10, allowed);
            assertEquals(90, rejected);

        } finally {
            executor.shutdownNow();
        }
    }
}
