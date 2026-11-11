package com.ratelimiter.controller;

import com.ratelimiter.algorithm.FixedWindowRateLimiter;
import com.ratelimiter.model.RateLimitResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {
    private final FixedWindowRateLimiter rateLimiter;

    public RateLimitController(FixedWindowRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/test-limit")
    public RateLimitResult testLimit(@RequestParam String userId) {
        return rateLimiter.allowRequest(userId);

    }
}
