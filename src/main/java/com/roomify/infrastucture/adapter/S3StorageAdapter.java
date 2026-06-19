package com.roomify.infrastucture.adapter;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.roomify.configuration.storage.StorageProperties;
import com.roomify.domain.spi.StorageSpi;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Slf4j
public class S3StorageAdapter implements StorageSpi {

    private final S3Client s3Client;
    private final StorageProperties props;

    public S3StorageAdapter(S3Client s3Client, StorageProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    @Override
    public String upload(byte[] data, @NonNull String key, @NonNull String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.debug("Uploaded object: key={}", key);
        return "%s/%s/%s".formatted(props.publicUrl(), props.bucket(), key);
    }

    @Override
    public void delete(@NonNull String url) {
        try {
            String key = url.replace(props.publicUrl() + "/" + props.bucket() + "/", "");
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.debug("Deleted object: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to delete object from storage: url={}", url, e);
        }
    }
}
