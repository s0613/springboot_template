package com.template.app.common.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.template.app.common.config.aws.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;

/**
 * Service for managing file operations with AWS S3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AmazonS3.class)
public class S3Service {

    private final AmazonS3 amazonS3;
    private final AwsS3Properties awsS3Properties;

    /**
     * Uploads a file to S3 and returns the file URL
     *
     * @param fileName    The name/path of the file in S3
     * @param fileBytes   The file content as byte array
     * @param contentType The MIME type of the file
     * @return The S3 URL of the uploaded file
     */
    public String uploadFile(String fileName, byte[] fileBytes, String contentType) {
        try {
            log.debug("Uploading file to S3: bucket={}, key={}", awsS3Properties.getBucketName(), fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(fileBytes.length);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            PutObjectRequest request = new PutObjectRequest(
                    awsS3Properties.getBucketName(),
                    fileName,
                    inputStream,
                    metadata
            );

            amazonS3.putObject(request);

            String fileUrl = buildFileUrl(fileName);
            log.info("File uploaded successfully to S3: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("Failed to upload file to S3: fileName={}", fileName, e);
            throw new RuntimeException("Failed to upload file to S3: " + fileName, e);
        }
    }

    /**
     * Deletes a file from S3
     *
     * @param fileName The name/path of the file in S3
     */
    public void deleteFile(String fileName) {
        try {
            log.debug("Deleting file from S3: bucket={}, key={}", awsS3Properties.getBucketName(), fileName);

            amazonS3.deleteObject(awsS3Properties.getBucketName(), fileName);

            log.info("File deleted successfully from S3: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to delete file from S3: fileName={}", fileName, e);
            throw new RuntimeException("Failed to delete file from S3: " + fileName, e);
        }
    }

    /**
     * Generates a presigned URL for secure file download
     *
     * @param fileName          The name/path of the file in S3
     * @param expirationMinutes How long the URL should be valid (in minutes)
     * @return The presigned URL
     */
    public String generatePresignedUrl(String fileName, int expirationMinutes) {
        try {
            log.debug("Generating presigned URL: bucket={}, key={}, expiration={} minutes",
                    awsS3Properties.getBucketName(), fileName, expirationMinutes);

            Date expiration = new Date();
            long expirationTimeMillis = expiration.getTime();
            expirationTimeMillis += 1000L * 60 * expirationMinutes;
            expiration.setTime(expirationTimeMillis);

            URL url = amazonS3.generatePresignedUrl(
                    awsS3Properties.getBucketName(),
                    fileName,
                    expiration
            );

            String presignedUrl = url.toString();
            log.info("Presigned URL generated successfully: {}", presignedUrl);

            return presignedUrl;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL: fileName={}", fileName, e);
            throw new RuntimeException("Failed to generate presigned URL: " + fileName, e);
        }
    }

    /**
     * Builds the file URL - uses CloudFront if configured, otherwise S3 direct URL
     *
     * @param fileName The name/path of the file in S3
     * @return The URL to access the file (CloudFront or S3)
     */
    private String buildFileUrl(String fileName) {
        String cloudfrontDomain = awsS3Properties.getCloudfrontDomain();
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            return String.format("https://%s/%s", cloudfrontDomain, fileName);
        }
        // Fallback to S3 direct URL if CloudFront is not configured
        return amazonS3.getUrl(awsS3Properties.getBucketName(), fileName).toString();
    }
}
