package com.ratelimiter.interceptor;

import com.ratelimiter.annotation.RateLimited;
import com.ratelimiter.config.RateLimitProperties;
import com.ratelimiter.model.RateLimitDecision;
import com.ratelimiter.model.RateLimitPolicy;

import com.ratelimiter.service.RateLimitExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitExecutionService rateLimitExecutionService;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitInterceptor(
            RateLimitExecutionService rateLimitExecutionService,
            RateLimitProperties rateLimitProperties
    ) {
        this.rateLimitExecutionService = rateLimitExecutionService;
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
        RateLimitDecision decision =
                rateLimitExecutionService.execute(
                        userId,
                        policyName,
                        policy
                );

        if(decision.rateLimitEvaluated()){
            response.setHeader("RateLimit-Limit", String.valueOf(policy.bucketCapacity()));
            response.setHeader("RateLimit-Remaining", String.valueOf(decision.remainingTokens()));
        } else {
            response.setHeader("RateLimit-Bypassed", "true");
        }

        if(!decision.allowed()){
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.getWriter().write( "{\"error\":\"Too Many Requests\"}");
            return false;
        }
        // Allow the request to reach its controller.
        return true;
    }
}
