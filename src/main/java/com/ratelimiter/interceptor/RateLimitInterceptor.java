package com.ratelimiter.interceptor;

import com.ratelimiter.annotation.RateLimited;
import com.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.ratelimiter.config.RateLimitProperties;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.TokenBucketResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitInterceptor(
            TokenBucketRateLimiter tokenBucketRateLimiter,
            RateLimitProperties rateLimitProperties
    ) {
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        // Ignore requests that are not handled by controller methods.
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check whether the controller method has @RateLimited.
        RateLimited annotation =
                handlerMethod.getMethodAnnotation(RateLimited.class);

        // If annotation is absent, allow the request.
        if (annotation == null) {
            return true;
        }

        String policyName = annotation.policy();

        // Find the configured policy.
        RateLimitProperties.Policy configuredPolicy =
                rateLimitProperties.policies().get(policyName);

        if (configuredPolicy == null) {
            throw new IllegalStateException(
                    "Unknown rate limit policy: " + policyName
            );
        }

        // Convert configuration into our domain policy.
        RateLimitPolicy policy = new RateLimitPolicy(
                configuredPolicy.capacity(),
                configuredPolicy.refillRate(),
                configuredPolicy.requestCost()
        );

        // For now, use the userId query parameter as the client identifier.
        String userId = request.getParameter("userId");

        if (userId == null || userId.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Missing userId parameter"
            );
            return false;
        }

        // Execute the Token Bucket algorithm.
        TokenBucketResult result =
                tokenBucketRateLimiter.allowRequest(
                        userId,
                        policyName,
                        policy
                );

        // Reject requests that exceed the configured limit.
        if (!result.isAllowed()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\"}"
            );
            return false;
        }

        // Allow the request to reach its controller.
        return true;
    }
}
