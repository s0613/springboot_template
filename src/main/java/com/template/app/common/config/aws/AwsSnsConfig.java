package com.template.app.common.config.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuration for AWS SNS (Simple Notification Service).
 * Provides SnsClient bean for push notifications.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsSnsConfig {

    @Value("${aws.sns.region}")
    private String region;

    @Value("${aws.sns.access-key:}")
    private String accessKey;

    @Value("${aws.sns.secret-key:}")
    private String secretKey;

    @Value("${aws.sns.use-iam-credentials:false}")
    private boolean useIamCredentials;

    @Value("${aws.sns.enabled:false}")
    private boolean enabled;

    /**
     * Creates SNS client bean.
     * Uses credentials from application.yml or IAM role if configured.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aws.sns", name = "enabled", havingValue = "true")
    public SnsClient snsClient() {
        log.info("Initializing AWS SNS client for region: {}", region);

        AwsCredentialsProvider credentialsProvider;

        if (useIamCredentials) {
            log.info("Using IAM instance profile credentials for SNS");
            credentialsProvider = InstanceProfileCredentialsProvider.create();
        } else if (accessKey != null && !accessKey.isEmpty()) {
            log.info("Using explicit AWS credentials for SNS");
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            credentialsProvider = StaticCredentialsProvider.create(awsCredentials);
        } else {
            throw new IllegalStateException("AWS SNS is enabled but no credentials configured");
        }

        SnsClient client = SnsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build();

        log.info("AWS SNS client initialized successfully");

        return client;
    }
}
