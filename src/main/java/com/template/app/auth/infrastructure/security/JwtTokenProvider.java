package com.template.app.auth.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity:3600000}") // 1 hour default
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:1209600000}") // 14 days default
    private long refreshTokenValidity;

    private final RedisTemplate<String, Object> redisTemplate;
    private SecretKey key;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    private static final int MIN_KEY_LENGTH_BYTES = 32; // 256 bits

    @PostConstruct
    public void init() {
        validateSecretKey();
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private void validateSecretKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured");
        }

        if (secretKey.startsWith("changeme")) {
            log.warn("SECURITY WARNING: Using default JWT secret. This is INSECURE for production!");
            if ("production".equals(activeProfile) || "prod".equals(activeProfile)) {
                throw new IllegalStateException("Default JWT secret not allowed in production");
            }
        }

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    String.format("JWT secret key must be at least %d bytes (256 bits). Current length: %d bytes",
                            MIN_KEY_LENGTH_BYTES, keyBytes.length)
            );
        }

        log.info("JWT secret key validated successfully");
    }

    public String generateAccessToken(String userId, String[] roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .setSubject(userId)
                .claim("roles", roles)
                .claim("type", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key) // Auto-detects algorithm from key
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidity);

        // Add a unique identifier to ensure tokens are different even if generated in same millisecond
        String jti = java.util.UUID.randomUUID().toString();

        String token = Jwts.builder()
                .setSubject(userId)
                .claim("type", "REFRESH")
                .claim("jti", jti)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key) // Auto-detects algorithm from key
                .compact();

        // Store in Redis for validation and rotation
        redisTemplate.opsForValue().set(
                "refresh_token:" + userId,
                token,
                refreshTokenValidity,
                TimeUnit.MILLISECONDS
        );

        return token;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        // Check if token is blacklisted
        if (isBlacklisted(token)) {
            log.warn("Token is blacklisted");
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = getClaims(token);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        "blacklist:" + token,
                        true,
                        ttl,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().get("blacklist:" + token)
        );
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String[] getRolesFromToken(String token) {
        @SuppressWarnings("unchecked")
        java.util.List<String> rolesList = (java.util.List<String>) getClaims(token).get("roles");
        return rolesList != null ? rolesList.toArray(new String[0]) : new String[0];
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenValidity() {
        return accessTokenValidity / 1000; // Convert to seconds
    }
}
