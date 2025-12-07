package com.template.app.common.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.burst-capacity:100}")
    private int burstCapacity;

    @Bean
    public LettuceBasedProxyManager<String> proxyManager() {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort);

        if (redisPassword != null && !redisPassword.isBlank()) {
            uriBuilder.withPassword(redisPassword.toCharArray());
        }

        RedisClient redisClient = RedisClient.create(uriBuilder.build());
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                io.lettuce.core.codec.RedisCodec.of(
                        io.lettuce.core.codec.StringCodec.UTF8,
                        io.lettuce.core.codec.ByteArrayCodec.INSTANCE
                )
        );

        log.info("Rate limiting initialized with Redis at {}:{}", redisHost, redisPort);
        log.info("Rate limit: {} requests/minute, burst capacity: {}", requestsPerMinute, burstCapacity);

        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(io.github.bucket4j.distributed.ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
                .build();
    }

    @Bean
    public Supplier<BucketConfiguration> bucketConfiguration() {
        return () -> {
            // Refill strategy: replenish tokens at a fixed rate
            Refill refill = Refill.intervally(requestsPerMinute, Duration.ofMinutes(1));

            // Bandwidth: maximum capacity and refill strategy
            Bandwidth limit = Bandwidth.classic(burstCapacity, refill);

            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }

    /**
     * Creates a specific bucket for authentication endpoints with stricter limits
     */
    public Supplier<BucketConfiguration> authBucketConfiguration() {
        return () -> {
            // Auth endpoints: 10 requests per minute
            Refill refill = Refill.intervally(10, Duration.ofMinutes(1));
            Bandwidth limit = Bandwidth.classic(20, refill); // Burst of 20

            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }
}
