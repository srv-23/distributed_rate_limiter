package com.ratelimiter.service;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class RedisHealthService {
    private final RedisConnectionFactory connectionFactory;

    public RedisHealthService(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String checkConnection(){
        try(RedisConnection connection = connectionFactory.getConnection()){
            return connection.ping();
        }
    }
}
