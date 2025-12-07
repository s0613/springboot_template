package com.template.app.common.integration.sms;

import com.template.app.common.config.NcpSensProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NcpSensClient {

    private final NcpSensProperties properties;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendSms(String to, String message) throws Exception {
        if (!properties.isEnabled()) {
            log.warn("NCP SENS is disabled. SMS would have been sent to {}: {}", to, message);
            return true; // Return true to allow verification flow in development/testing
        }

        // NCP SENS requires phone number without dashes
        String normalizedPhone = to.replaceAll("-", "");

        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = makeSignature(timestamp);

        String url = String.format("%s/sms/v2/services/%s/messages",
            properties.getApiUrl(), properties.getServiceId());

        Map<String, Object> requestBody = Map.of(
            "type", properties.getSms().getType(),
            "contentType", properties.getSms().getContentType(),
            "countryCode", "82",
            "from", properties.getFromPhone(),
            "content", message,
            "messages", List.of(Map.of("to", normalizedPhone))
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("x-ncp-apigw-timestamp", timestamp)
            .header("x-ncp-iam-access-key", properties.getAccessKey())
            .header("x-ncp-apigw-signature-v2", signature)
            .POST(HttpRequest.BodyPublishers.ofString(
                objectMapper.writeValueAsString(requestBody),
                StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 202) {
            log.info("SMS sent successfully to {}", to);
            return true;
        } else {
            log.error("Failed to send SMS. Status: {}, Body: {}",
                response.statusCode(), response.body());
            return false;
        }
    }

    private String makeSignature(String timestamp) throws Exception {
        String space = " ";
        String newLine = "\n";
        String method = "POST";
        String url = "/sms/v2/services/" + properties.getServiceId() + "/messages";

        String message = new StringBuilder()
            .append(method)
            .append(space)
            .append(url)
            .append(newLine)
            .append(timestamp)
            .append(newLine)
            .append(properties.getAccessKey())
            .toString();

        SecretKeySpec signingKey = new SecretKeySpec(
            properties.getSecretKey().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(rawHmac);
    }
}
