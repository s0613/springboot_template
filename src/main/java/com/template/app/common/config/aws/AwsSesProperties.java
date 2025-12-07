package com.template.app.common.config.aws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AWS SES (Simple Email Service).
 * Binds to application.yml properties under 'aws.ses'.
 */
@Configuration
@ConfigurationProperties(prefix = "aws.ses")
@Getter
@Setter
public class AwsSesProperties {

    /**
     * AWS region for SES
     */
    private String region = "ap-northeast-2";

    /**
     * AWS access key
     */
    private String accessKey;

    /**
     * AWS secret key
     */
    private String secretKey;

    /**
     * Sender email address (must be verified in SES)
     */
    private String fromEmail = "noreply@cogmo-annyeong.com";

    /**
     * Sender display name
     */
    private String fromName = "코그모 안녕";

    /**
     * Whether to use AWS credentials from environment/IAM role
     */
    private boolean useIamCredentials = false;
}
