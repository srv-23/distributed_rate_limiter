
package com.ratelimiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistributedRateLimiterIntegrationTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Pattern ALLOWED_PATTERN =
            Pattern.compile("\"allowed\"\\s*:\\s*true");

    @Test
    void shouldEnforceRateLimitAcrossTwoInstances() {

        // Run this test only when explicitly requested.
        Assumptions.assumeTrue(
                Boolean.getBoolean("runDistributedIntegration"),
                "Run with -DrunDistributedIntegration=true"
        );

        String userId = "distributed-concurrency-101";

        int totalRequests = 100;

        List<CompletableFuture<String>> futures =
                IntStream.range(0, totalRequests)
                        .mapToObj(i -> {

                            int port = (i % 2 == 0) ? 8080 : 8081;

                            String url = "http://localhost:" + port
                                    + "/api/test-token-bucket"
                                    + "?userId=" + userId
                                    + "&policyName=distributed-test";

                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .GET()
                                    .build();

                            return httpClient.sendAsync(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            ).thenApply(response -> {

                                assertEquals(
                                        200,
                                        response.statusCode(),
                                        "Unexpected HTTP status from port " + port
                                );

                                return response.body();
                            });
                        })
                        .toList();

        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        ).join();

        long allowed = futures.stream()
                .map(CompletableFuture::join)
                .filter(body -> ALLOWED_PATTERN.matcher(body).find())
                .count();

        long rejected = totalRequests - allowed;

        System.out.println("Total requests: " + totalRequests);
        System.out.println("Allowed requests: " + allowed);
        System.out.println("Rejected requests: " + rejected);

        assertEquals(10, allowed);
        assertEquals(90, rejected);
    }
}
