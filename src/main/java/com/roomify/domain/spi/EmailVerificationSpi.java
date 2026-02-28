package com.roomify.domain.spi;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.user.EmailVerificationToken;

public interface EmailVerificationSpi {

    void insertEmailVerification(@NonNull EmailVerificationToken emailToken);

    Optional<EmailVerificationToken> findEmailVerification(@NonNull String hashedToken);

    void deleteEmailVerification(@NonNull EmailVerificationToken emailVerificationToken);

    int deleteAllExpiredTokenBefore(@NonNull LocalDateTime threshold);

}
