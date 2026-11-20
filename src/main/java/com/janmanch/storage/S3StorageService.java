package com.janmanch.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Stores files in a private S3 bucket. Credentials come from the default AWS
 * chain (the EC2 instance role when deployed, or environment variables locally).
 */
@Service
@ConditionalOnProperty(name = "janmanch.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final Duration URL_VALIDITY = Duration.ofMinutes(60);

    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;

    public S3StorageService(@Value("${janmanch.storage.s3.bucket}") String bucket,
                            @Value("${janmanch.storage.s3.region}") String region) {
        this.bucket = bucket;
        this.s3 = S3Client.builder().region(Region.of(region)).build();
        this.presigner = S3Presigner.builder().region(Region.of(region)).build();
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) throws IOException {
        try {
            s3.putObject(r -> r.bucket(bucket).key(key).contentType(contentType),
                    RequestBody.fromInputStream(content, size));
        } catch (SdkException ex) {
            throw new IOException("Upload to S3 failed for " + key, ex);
        }
    }

    @Override
    public Path fetchToLocal(String key) throws IOException {
        Path dir = Files.createTempDirectory("janmanch-");
        Path file = dir.resolve("source");
        try {
            s3.getObject(r -> r.bucket(bucket).key(key), ResponseTransformer.toFile(file));
        } catch (SdkException ex) {
            throw new IOException("Download from S3 failed for " + key, ex);
        }
        return file;
    }

    @Override
    public void releaseLocal(Path path) {
        try {
            Files.deleteIfExists(path);
            Files.deleteIfExists(path.getParent());
        } catch (IOException ex) {
            log.warn("Could not remove temporary file {}", path, ex);
        }
    }

    @Override
    public String urlFor(String key) {
        // The bucket is private; the browser gets a link that expires after an hour.
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(URL_VALIDITY)
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(r -> r.bucket(bucket).key(key));
        } catch (SdkException ex) {
            log.warn("Could not delete {} from S3", key, ex);
        }
    }
}
