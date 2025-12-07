package com.template.app.auth.infrastructure.oauth2.kakao;

import com.template.app.auth.api.dto.response.OAuth2UserInfo;
import com.template.app.auth.infrastructure.exception.InvalidOAuth2TokenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
public class KakaoTokenVerifierService {

    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer verificationTimer;

    public KakaoTokenVerifierService(RestTemplate restTemplate,
                                      ObjectMapper objectMapper,
                                      MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        this.successCounter = Counter.builder("oauth.kakao.verify.success")
                .description("Successful Kakao token verifications")
                .register(meterRegistry);

        this.failureCounter = Counter.builder("oauth.kakao.verify.failure")
                .description("Failed Kakao token verifications")
                .register(meterRegistry);

        this.verificationTimer = Timer.builder("oauth.kakao.verify.duration")
                .description("Time taken to verify Kakao token")
                .register(meterRegistry);
    }

    /**
     * Verify Kakao access token and extract user information
     *
     * @param accessToken Kakao access token from native SDK
     * @return OAuth2UserInfo with user details
     * @throws InvalidOAuth2TokenException if token is invalid
     */
    public OAuth2UserInfo verifyAccessToken(String accessToken) {
        return verificationTimer.record(() -> {
            try {
                OAuth2UserInfo result = verifyAccessTokenInternal(accessToken);
                successCounter.increment();
                return result;
            } catch (Exception e) {
                failureCounter.increment();
                throw e;
            }
        });
    }

    private OAuth2UserInfo verifyAccessTokenInternal(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidOAuth2TokenException("Access token cannot be null or empty");
        }

        try {
            // Call Kakao user info endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URI,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse response
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            String userId = rootNode.path("id").asText();
            JsonNode kakaoAccount = rootNode.path("kakao_account");
            JsonNode profile = kakaoAccount.path("profile");

            String email = kakaoAccount.path("email").asText(null);
            // Generate temporary email if not provided (Kakao email is optional)
            if (email == null || email.isBlank()) {
                email = "oauth.kakao." + userId + "@noreply.cogmo.life";
                log.debug("Generated temporary email for Kakao user: {}", email);
            }
            String name = profile.path("nickname").asText(null);
            String pictureUrl = profile.path("profile_image_url").asText(null);

            log.info("Successfully verified Kakao access token for user: {}", userId);

            return OAuth2UserInfo.builder()
                    .providerId(userId)
                    .provider("kakao")
                    .email(email)
                    .name(name)
                    .picture(pictureUrl)
                    .build();

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized Kakao access token", e);
            throw new InvalidOAuth2TokenException("Invalid Kakao access token", e);
        } catch (IOException e) {
            log.error("Failed to parse Kakao response", e);
            throw new InvalidOAuth2TokenException("Failed to parse Kakao response", e);
        } catch (RestClientException e) {
            log.error("Network error while verifying Kakao access token", e);
            throw new InvalidOAuth2TokenException("Network error while verifying Kakao access token", e);
        }
    }
}
