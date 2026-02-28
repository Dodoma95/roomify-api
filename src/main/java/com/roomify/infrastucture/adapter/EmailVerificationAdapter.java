package com.roomify.infrastucture.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.roomify.domain.spi.EmailVerificationSpi;
import com.roomify.infrastucture.models.user.EmailVerificationToken;
import com.roomify.infrastucture.repository.EmailVerificationRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmailVerificationAdapter implements EmailVerificationSpi {

    private final EmailVerificationRepository emailVerificationRepository;

    public EmailVerificationAdapter(EmailVerificationRepository emailVerificationRepository) {
        this.emailVerificationRepository = emailVerificationRepository;
    }

    @Override
    public void insertEmailVerification(@NonNull EmailVerificationToken emailToken) {
        log.debug("Inserting email verification token for user ID: {}", emailToken.getUser().getId());
        emailVerificationRepository.save(emailToken);
    }

    @Override
    public Optional<EmailVerificationToken> findEmailVerification(@NonNull String hashedToken) {
        log.debug("Searching EmailVerificationToken for hashed: {}", hashedToken);
        return emailVerificationRepository.findByToken(hashedToken);
    }

    @Override
    public void deleteEmailVerification(@NonNull EmailVerificationToken emailVerificationToken) {
        log.debug("Deleting email verification token for user ID: {}", emailVerificationToken.getUser().getId());
        emailVerificationRepository.delete(emailVerificationToken);
    }

    @Override
    public int deleteAllExpiredTokenBefore(@NonNull LocalDateTime threshold) {
        return emailVerificationRepository.deleteAllExpiredBefore(threshold);
    }
}
