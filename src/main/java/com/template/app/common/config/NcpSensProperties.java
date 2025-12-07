package com.template.app.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ncp.sens")
public class NcpSensProperties {
    private String serviceId;
    private String accessKey;
    private String secretKey;
    private String fromPhone;
    private boolean enabled = false;
    private String apiUrl;
    private SmsConfig sms = new SmsConfig();
    private RetryConfig retry = new RetryConfig();

    @Getter
    @Setter
    public static class SmsConfig {
        private String type = "SMS";
        private String contentType = "COMM";
    }

    @Getter
    @Setter
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMs = 1000;
    }
}
