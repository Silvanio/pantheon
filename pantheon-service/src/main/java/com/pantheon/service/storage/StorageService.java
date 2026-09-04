package com.pantheon.service.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Thin wrapper around an S3-compatible object storage client (MinIO locally, S3-compatible in
 * any other environment) — see add-daily-report-media-and-signoff's design.md "Object storage".
 * Uploaded files are stored here, never as database rows.
 */
@Component
public class StorageService {

    private final S3Client s3Client;
    private final String bucket;

    public StorageService(
            @Value("${pantheon.storage.endpoint}") String endpoint,
            @Value("${pantheon.storage.region}") String region,
            @Value("${pantheon.storage.bucket}") String bucket,
            @Value("${pantheon.storage.access-key}") String accessKey,
            @Value("${pantheon.storage.secret-key}") String secretKey) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    public void putObject(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    public byte[] getObject(String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }
}
