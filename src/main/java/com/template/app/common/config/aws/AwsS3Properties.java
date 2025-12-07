package com.template.app.common.config.aws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AWS S3
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class AwsS3Properties {
    private String bucketName;
    private String region;
    private String accessKey;
    private String secretKey;
    private int presignedUrlExpiration = 60; // minutes
    private String cloudfrontDomain; // CloudFront distribution domain (e.g., d276rpin66wbui.cloudfront.net)
}
