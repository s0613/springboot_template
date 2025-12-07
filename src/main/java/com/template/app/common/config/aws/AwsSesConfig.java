package com.template.app.common.config.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for AWS SES (Simple Email Service).
 * Provides AmazonSimpleEmailService client bean.
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AwsSesConfig {

    private final AwsSesProperties sesProperties;

    /**
     * Creates AmazonSimpleEmailService client.
     * Uses credentials from application.yml or IAM role if configured.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aws.ses", name = "enabled", havingValue = "true")
    public AmazonSimpleEmailService amazonSimpleEmailService() {
        log.info("Initializing AWS SES client for region: {}", sesProperties.getRegion());

        AmazonSimpleEmailServiceClientBuilder builder = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(sesProperties.getRegion());

        // Use explicit credentials if provided, otherwise use default credential chain (IAM role, etc.)
        if (!sesProperties.isUseIamCredentials() &&
            sesProperties.getAccessKey() != null &&
            !sesProperties.getAccessKey().isEmpty()) {

            log.info("Using explicit AWS credentials for SES");
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
                    sesProperties.getAccessKey(),
                    sesProperties.getSecretKey()
            );
            builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        } else {
            log.info("Using default AWS credential chain (IAM role/environment) for SES");
        }

        AmazonSimpleEmailService sesClient = builder.build();

        log.info("AWS SES client initialized successfully. From email: {}", sesProperties.getFromEmail());

        return sesClient;
    }
}
