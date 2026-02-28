package com.roomify.domain.spi;

import org.jspecify.annotations.NonNull;

public interface EmailSenderSpi {
    void sendEmail(
            @NonNull String to,
            @NonNull String subject,
            @NonNull String content
    );
}
