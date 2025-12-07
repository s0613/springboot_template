package com.template.app.auth.infrastructure.oauth2.google;

import com.template.app.auth.api.dto.response.OAuth2UserInfo;
import com.template.app.auth.infrastructure.exception.InvalidOAuth2TokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Service
public class GoogleTokenVerifierService {

    private final Set<String> allowedClientIds;
    private final GoogleIdTokenVerifier verifier;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer verificationTimer;

    public GoogleTokenVerifierService(
            MeterRegistry meterRegistry,
            @Value("${app.oauth2.google.client-id-ios:}") String iosClientId,
            @Value("${app.oauth2.google.client-id-android:}") String androidClientId,
            @Value("${app.oauth2.google.client-id-server:}") String serverClientId,
            @Value("${app.oauth2.google.client-id-web:}") String webClientId) {

        this.allowedClientIds = new HashSet<>();
        if (iosClientId != null && !iosClientId.isBlank()) {
            allowedClientIds.add(iosClientId);
        }
        if (androidClientId != null && !androidClientId.isBlank()) {
            allowedClientIds.add(androidClientId);
        }
        if (serverClientId != null && !serverClientId.isBlank()) {
            allowedClientIds.add(serverClientId);
        }
        if (webClientId != null && !webClientId.isBlank()) {
            allowedClientIds.add(webClientId);
        }

        log.info("GoogleTokenVerifierService initialized with {} allowed client IDs", allowedClientIds.size());

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        ).setAudience(allowedClientIds).build();

        this.successCounter = Counter.builder("oauth.google.verify.success")
                .description("Successful Google token verifications")
                .register(meterRegistry);

        this.failureCounter = Counter.builder("oauth.google.verify.failure")
                .description("Failed Google token verifications")
                .register(meterRegistry);

        this.verificationTimer = Timer.builder("oauth.google.verify.duration")
                .description("Time taken to verify Google token")
                .register(meterRegistry);
    }

    /**
     * Verify Google ID token and extract user information
     *
     * @param idToken Google ID token from native SDK
     * @return OAuth2UserInfo with user details
     * @throws InvalidOAuth2TokenException if token is invalid
     */
    public OAuth2UserInfo verifyIdToken(String idToken) {
        return verificationTimer.record(() -> {
            try {
                OAuth2UserInfo result = verifyIdTokenInternal(idToken);
                successCounter.increment();
                return result;
            } catch (Exception e) {
                failureCounter.increment();
                throw e;
            }
        });
    }

    private OAuth2UserInfo verifyIdTokenInternal(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidOAuth2TokenException("ID token cannot be null or empty");
        }

        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                throw new InvalidOAuth2TokenException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            // Audience is already verified by GoogleIdTokenVerifier.setAudience()
            // Log which client ID was used for debugging
            log.debug("Token audience: {}", payload.getAudience());

            String userId = payload.getSubject();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            if (emailVerified == null || !emailVerified) {
                log.warn("Email not verified for user: {}", userId);
            }

            log.info("Successfully verified Google ID token for user: {}", userId);

            return OAuth2UserInfo.builder()
                    .providerId(userId)
                    .provider("google")
                    .email(email)
                    .name(name)
                    .picture(pictureUrl)
                    .build();

        } catch (InvalidOAuth2TokenException e) {
            throw e;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            log.error("Failed to verify Google ID token", e);
            throw new InvalidOAuth2TokenException("Failed to verify Google ID token", e);
        }
    }
}
