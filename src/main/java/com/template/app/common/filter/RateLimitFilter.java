package com.template.app.common.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RateLimitFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfiguration;

    @org.springframework.beans.factory.annotation.Value("${app.rate-limit.burst-capacity:100}")
    private int burstCapacity;

    private static final Set<String> AUTH_ENDPOINTS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/refresh"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientId = getClientId(request);
        String bucketKey = "rate_limit:" + clientId;

        // Get or create bucket for this client
        Bucket bucket = proxyManager.builder().build(bucketKey, bucketConfiguration);

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request allowed - add rate limit headers
            response.addHeader("X-RateLimit-Limit", String.valueOf(burstCapacity));
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-RateLimit-Reset", String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));

            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

            log.warn("Rate limit exceeded for client: {}. Retry after {} seconds", clientId, waitForRefill);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-RateLimit-Limit", String.valueOf(burstCapacity));
            response.addHeader("X-RateLimit-Remaining", "0");
            response.addHeader("X-RateLimit-Reset", String.valueOf(waitForRefill));
            response.addHeader("Retry-After", String.valueOf(waitForRefill));

            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after %d seconds.\",\"retryAfter\":%d}",
                    waitForRefill, waitForRefill
            ));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip rate limiting for actuator endpoints
        if (path.startsWith("/actuator")) {
            return true;
        }

        // Skip rate limiting for static resources
        if (path.startsWith("/static") || path.startsWith("/public")) {
            return true;
        }

        return false;
    }

    /**
     * Get client identifier for rate limiting.
     * Priority: User ID > IP Address
     */
    private String getClientId(HttpServletRequest request) {
        // Try to get authenticated user ID from request attribute (set by JWT filter)
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }

        // Fall back to IP address for unauthenticated requests
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // For auth endpoints, use IP-based rate limiting
        if (AUTH_ENDPOINTS.contains(path)) {
            return "auth_ip:" + clientIp;
        }

        return "ip:" + clientIp;
    }

    /**
     * Get client IP address, considering proxy headers
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
