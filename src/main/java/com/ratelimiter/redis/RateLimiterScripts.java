package com.ratelimiter.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RateLimiterScripts {
    private final DefaultRedisScript<List> fixedWindowScript;
    private final DefaultRedisScript<List> tokenBucketScript;

    public RateLimiterScripts(){
        fixedWindowScript = new DefaultRedisScript<>();
        fixedWindowScript.setLocation(new ClassPathResource("scripts/fixed_window.lua"));
        fixedWindowScript.setResultType(List.class);

        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        tokenBucketScript.setResultType(List.class);
    }

    public DefaultRedisScript<List> getFixedWindowScript() {
        return fixedWindowScript;
    }

    public DefaultRedisScript<List> getTokenBucketScript() {
        return tokenBucketScript;
    }
}
