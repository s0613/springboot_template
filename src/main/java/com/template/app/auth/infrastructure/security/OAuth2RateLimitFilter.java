package com.template.app.auth.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class OAuth2RateLimitFilter extends OncePerRequestFilter {

    private final int requestsPerMinute;
    private final Cache<String, Bucket> buckets;

    public OAuth2RateLimitFilter(
            @Value("${app.rate-limit.oauth2.requests-per-minute:10}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(10_000)
                .build();

        log.info("OAuth2RateLimitFilter initialized with {} requests per minute", requestsPerMinute);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Only apply to OAuth2 verification endpoints
        if (!uri.contains("/oauth2/google/verify") &&
            !uri.contains("/oauth2/kakao/verify") &&
            !uri.contains("/oauth2/apple/verify")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientId(request);
        Bucket bucket = buckets.get(clientId, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {}", clientId);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private String getClientId(HttpServletRequest request) {
        // Try X-Forwarded-For for proxy scenarios
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Take the first IP (original client)
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
