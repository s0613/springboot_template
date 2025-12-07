package com.template.app.file.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.template.app.common.config.aws.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
public class S3FileStorageService implements FileStorageService {

    private final AmazonS3 amazonS3;
    private final AwsS3Properties awsS3Properties;

    @Override
    public String upload(MultipartFile file, String path, String filename) {
        try {
            String key = buildKey(path, filename);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3.putObject(new PutObjectRequest(
                    awsS3Properties.getBucketName(),
                    key,
                    file.getInputStream(),
                    metadata
            ));

            log.info("File uploaded to S3: {}", key);
            return key;
        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public String upload(byte[] bytes, String path, String filename, String contentType) {
        String key = buildKey(path, filename);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(bytes.length);

        amazonS3.putObject(new PutObjectRequest(
                awsS3Properties.getBucketName(),
                key,
                new ByteArrayInputStream(bytes),
                metadata
        ));

        log.info("File uploaded to S3: {}", key);
        return key;
    }

    @Override
    public InputStream download(String filePath) {
        try {
            S3Object s3Object = amazonS3.getObject(awsS3Properties.getBucketName(), filePath);
            return s3Object.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to download file from S3: {}", filePath, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    public boolean delete(String filePath) {
        try {
            amazonS3.deleteObject(awsS3Properties.getBucketName(), filePath);
            log.info("File deleted from S3: {}", filePath);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String filePath) {
        try {
            return amazonS3.doesObjectExist(awsS3Properties.getBucketName(), filePath);
        } catch (Exception e) {
            log.error("Failed to check file existence in S3: {}", filePath, e);
            return false;
        }
    }

    @Override
    public String getPublicUrl(String filePath) {
        // Use CloudFront domain if configured, otherwise use S3 URL
        if (awsS3Properties.getCloudfrontDomain() != null && !awsS3Properties.getCloudfrontDomain().isEmpty()) {
            return String.format("https://%s/%s", awsS3Properties.getCloudfrontDomain(), filePath);
        }
        return amazonS3.getUrl(awsS3Properties.getBucketName(), filePath).toString();
    }

    @Override
    public String getPresignedUrl(String filePath, Duration duration) {
        Date expiration = new Date(System.currentTimeMillis() + duration.toMillis());

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                awsS3Properties.getBucketName(),
                filePath
        )
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        return amazonS3.generatePresignedUrl(request).toString();
    }

    @Override
    public String getPresignedUploadUrl(String filePath, String contentType, Duration duration) {
        Date expiration = new Date(System.currentTimeMillis() + duration.toMillis());

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                awsS3Properties.getBucketName(),
                filePath
        )
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);

        return amazonS3.generatePresignedUrl(request).toString();
    }

    private String buildKey(String path, String filename) {
        if (path == null || path.isEmpty()) {
            return filename;
        }
        // Remove leading/trailing slashes and combine
        String cleanPath = path.replaceAll("^/+|/+$", "");
        return cleanPath + "/" + filename;
    }
}
