package com.ratelimiter.controller;

import com.ratelimiter.service.RedisHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final RedisHealthService redisHealthService;

    public HealthController(RedisHealthService redisHealthService) {
        this.redisHealthService = redisHealthService;
    }

    @GetMapping("/health/redis")
    public String redisHealth() {
        return redisHealthService.checkConnection();
    }
}
