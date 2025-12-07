package com.template.app.auth.infrastructure.oauth2.apple;

import com.template.app.auth.api.dto.response.OAuth2UserInfo;
import com.template.app.auth.infrastructure.exception.InvalidOAuth2TokenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AppleTokenVerifierService {

    private static final String APPLE_KEYS_URI = "https://appleid.apple.com/auth/keys";
    private static final String CACHE_KEY = "apple_public_keys";

    private final String appleClientId;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer verificationTimer;

    // Cache Apple's public keys for 1 hour
    private final Cache<String, List<Map<String, Object>>> keyCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    public AppleTokenVerifierService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${spring.security.oauth2.client.registration.apple.client-id:com.template.app}") String appleClientId) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.appleClientId = appleClientId;

        log.info("AppleTokenVerifierService initialized with client ID: {}", appleClientId);

        this.successCounter = Counter.builder("oauth.apple.verify.success")
                .description("Successful Apple token verifications")
                .register(meterRegistry);

        this.failureCounter = Counter.builder("oauth.apple.verify.failure")
                .description("Failed Apple token verifications")
                .register(meterRegistry);

        this.cacheHitCounter = Counter.builder("oauth.apple.keys.cache.hit")
                .description("Apple public keys cache hits")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("oauth.apple.keys.cache.miss")
                .description("Apple public keys cache misses")
                .register(meterRegistry);

        this.verificationTimer = Timer.builder("oauth.apple.verify.duration")
                .description("Time taken to verify Apple token")
                .register(meterRegistry);
    }

    /**
     * Verify Apple ID token and extract user information
     *
     * @param idToken Apple ID token from native SDK
     * @param providedName User's name from Apple (only provided on first login)
     * @param providedEmail User's email from Apple (may be provided separately)
     * @return OAuth2UserInfo with user details
     * @throws InvalidOAuth2TokenException if token is invalid
     */
    public OAuth2UserInfo verifyIdToken(String idToken, String providedName, String providedEmail) {
        return verificationTimer.record(() -> {
            try {
                OAuth2UserInfo result = verifyIdTokenInternal(idToken, providedName, providedEmail);
                successCounter.increment();
                return result;
            } catch (Exception e) {
                failureCounter.increment();
                throw e;
            }
        });
    }

    private OAuth2UserInfo verifyIdTokenInternal(String idToken, String providedName, String providedEmail) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidOAuth2TokenException("ID token cannot be null or empty");
        }

        try {
            // Fetch Apple's public keys (with caching)
            List<Map<String, Object>> publicKeys = getApplePublicKeys();

            // Verify token signature
            String userId = verifyTokenSignature(idToken, publicKeys);

            // Decode JWT without verification to extract claims
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new InvalidOAuth2TokenException("Invalid token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(payload);

            // Extract user information from ID token
            String tokenEmail = claims.path("email").asText(null);
            Boolean emailVerified = claims.path("email_verified").asBoolean(false);
            String tokenName = claims.path("name").asText(null);

            // Use provided email if token email is null
            String email = (tokenEmail != null) ? tokenEmail : providedEmail;

            // Use provided name if token name is null
            String name = (tokenName != null) ? tokenName : providedName;

            // Relaxed email verification: log warning instead of throwing exception
            if (email != null && !emailVerified) {
                log.warn("Email not verified for Apple user: {} (email: {})", userId, email);
            }

            // Generate fallback email if no email is available
            if (email == null) {
                email = "oauth.apple." + userId + "@noreply.cogmo.life";
                log.info("Generated fallback email for Apple user: {}", email);
            }

            log.info("Successfully verified Apple ID token for user: {} (email: {})", userId, email);

            return OAuth2UserInfo.builder()
                    .providerId(userId)
                    .provider("apple")
                    .email(email)
                    .name(name)
                    .picture(null) // Apple doesn't provide picture URL
                    .build();

        } catch (InvalidOAuth2TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Apple ID token", e);
            throw new InvalidOAuth2TokenException("Failed to verify Apple ID token", e);
        }
    }

    private List<Map<String, Object>> getApplePublicKeys() {
        List<Map<String, Object>> cachedKeys = keyCache.getIfPresent(CACHE_KEY);
        if (cachedKeys != null) {
            cacheHitCounter.increment();
            log.debug("Apple public keys cache hit");
            return cachedKeys;
        }

        cacheMissCounter.increment();
        log.debug("Apple public keys cache miss, fetching from Apple");
        List<Map<String, Object>> keys = fetchApplePublicKeys();
        keyCache.put(CACHE_KEY, keys);
        return keys;
    }

    private List<Map<String, Object>> fetchApplePublicKeys() {
        try {
            String response = restTemplate.getForObject(new URI(APPLE_KEYS_URI), String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode keys = root.path("keys");

            if (keys.isMissingNode()) {
                throw new InvalidOAuth2TokenException("No keys found in Apple's public key response");
            }

            return objectMapper.convertValue(keys, List.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch Apple public keys", e);
            throw new InvalidOAuth2TokenException("Failed to fetch Apple public keys", e);
        } catch (Exception e) {
            log.error("Failed to parse Apple public keys response", e);
            throw new InvalidOAuth2TokenException("Failed to parse Apple public keys response", e);
        }
    }

    private String verifyTokenSignature(String idToken, List<Map<String, Object>> publicKeys) {
        for (Map<String, Object> keyData : publicKeys) {
            try {
                String kid = (String) keyData.get("kid");
                String kty = (String) keyData.get("kty");
                String use = (String) keyData.get("use");

                if (!"RSA".equals(kty) || !"sig".equals(use)) {
                    continue;
                }

                // Reconstruct public key from JWK
                PublicKey publicKey = reconstructPublicKey(keyData);

                // Verify JWT signature
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(idToken)
                        .getPayload();

                // Verify audience (aud should be our client ID)
                Object aud = claims.get("aud");
                if (aud instanceof List) {
                    if (!((List<?>) aud).contains(appleClientId)) {
                        log.warn("Token audience mismatch. Expected: {}, Got: {}", appleClientId, aud);
                        throw new InvalidOAuth2TokenException("Token audience mismatch");
                    }
                } else if (!appleClientId.equals(aud)) {
                    log.warn("Token audience mismatch. Expected: {}, Got: {}", appleClientId, aud);
                    throw new InvalidOAuth2TokenException("Token audience mismatch");
                }

                return claims.getSubject();

            } catch (InvalidOAuth2TokenException e) {
                throw e;
            } catch (Exception e) {
                log.debug("Key {} failed verification, trying next key", (String) keyData.get("kid"));
                continue;
            }
        }

        throw new InvalidOAuth2TokenException("No valid key found to verify token signature");
    }

    private PublicKey reconstructPublicKey(Map<String, Object> keyData) throws Exception {
        String n = (String) keyData.get("n");
        String e = (String) keyData.get("e");

        if (n == null || e == null) {
            throw new InvalidOAuth2TokenException("Invalid JWK format");
        }

        // Decode base64url encoded values
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        // Create BigInteger values for RSA modulus and exponent
        java.math.BigInteger modulus = new java.math.BigInteger(1, nBytes);
        java.math.BigInteger exponent = new java.math.BigInteger(1, eBytes);

        // Create RSA public key spec
        java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(modulus, exponent);

        // Generate public key
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}
