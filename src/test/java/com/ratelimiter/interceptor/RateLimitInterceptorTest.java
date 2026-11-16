
package com.ratelimiter.interceptor;

import com.ratelimiter.annotation.RateLimited;
import com.ratelimiter.config.RateLimitProperties;
import com.ratelimiter.model.RateLimitDecision;
import com.ratelimiter.service.RateLimitExecutionService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private final RateLimitExecutionService executionService =
            mock(RateLimitExecutionService.class);

    private RateLimitProperties createProperties() {
        return new RateLimitProperties(
                "fail-closed",
                Map.of(
                        "profile",
                        new RateLimitProperties.Policy(10, 2, 1)
                )
        );
    }

    private RateLimitInterceptor createInterceptor() {
        return new RateLimitInterceptor(
                executionService,
                createProperties()
        );
    }

    @Test
    void shouldRejectRequestWhenUserIdIsMissing() throws Exception {

        RateLimitInterceptor interceptor = createInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TestController controller = new TestController();

        HandlerMethod handler = new HandlerMethod(
                controller,
                controller.getClass().getMethod("profile")
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                handler
        );

        assertFalse(result);
        assertEquals(400, response.getStatus());

        verifyNoInteractions(executionService);
    }

    @Test
    void shouldRejectUnknownPolicy() throws Exception {

        RateLimitInterceptor interceptor = createInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TestController controller = new TestController();

        HandlerMethod handler = new HandlerMethod(
                controller,
                controller.getClass().getMethod("unknownPolicy")
        );

        request.setParameter("userId", "101");

        assertThrows(
                IllegalStateException.class,
                () -> interceptor.preHandle(request, response, handler)
        );

        verifyNoInteractions(executionService);
    }

    @Test
    void shouldAllowValidRequestToContinue() throws Exception {

        RateLimitInterceptor interceptor = createInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TestController controller = new TestController();

        HandlerMethod handler = new HandlerMethod(
                controller,
                controller.getClass().getMethod("profile")
        );

        request.setParameter("userId", "101");

        when(executionService.execute(
                eq("101"),
                eq("profile"),
                any()
        )).thenReturn(
                new RateLimitDecision(true, true, 9.0, 0)
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                handler
        );

        assertTrue(result);
        assertEquals("10.0", response.getHeader("RateLimit-Limit"));
        assertEquals("9.0", response.getHeader("RateLimit-Remaining"));

        verify(executionService).execute(
                eq("101"),
                eq("profile"),
                any()
        );
    }

    public static class TestController {

        @RateLimited(policy = "profile")
        public void profile() {
        }

        @RateLimited(policy = "missing")
        public void unknownPolicy() {
        }
    }


    @Test
    void shouldRejectRequestWhenRateLimitIsExceeded() throws Exception {

        RateLimitInterceptor interceptor = createInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                "profile"
        );

        request.setParameter("userId", "101");

        when(executionService.execute(
                eq("101"),
                eq("profile"),
                any()
        )).thenReturn(
                new RateLimitDecision(false, true, 0.0, 5)
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                handler
        );

        assertFalse(result);
        assertEquals(429, response.getStatus());
        assertEquals("5", response.getHeader("Retry-After"));
        assertEquals(
                "{\"error\":\"Too Many Requests\"}",
                response.getContentAsString()
        );
    }

    @Test
    void shouldAddBypassHeaderWhenRateLimitIsNotEvaluated() throws Exception {

        RateLimitInterceptor interceptor = createInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        HandlerMethod handler = new HandlerMethod(
                new TestController(),
                "profile"
        );

        request.setParameter("userId", "101");

        when(executionService.execute(
                eq("101"),
                eq("profile"),
                any()
        )).thenReturn(
                new RateLimitDecision(true, false, null, 0)
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                handler
        );

        assertTrue(result);
        assertEquals(
                "true",
                response.getHeader("RateLimit-Bypassed")
        );
    }

}
