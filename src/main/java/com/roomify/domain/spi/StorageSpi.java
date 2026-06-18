package com.roomify.domain.spi;

import org.jspecify.annotations.NonNull;

public interface StorageSpi {

    /**
     * Upload bytes to storage and return the public URL.
     */
    String upload(byte[] data, @NonNull String key, @NonNull String contentType);

    /**
     * Delete object identified by its public URL (best-effort, no exception thrown on failure).
     */
    void delete(@NonNull String url);
}
