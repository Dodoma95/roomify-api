package com.roomify.domain.service.auth;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.spi.EmailVerificationSpi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final EmailVerificationSpi emailVerificationSpi;

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredEmailVerificationTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(1);
        int deleted = emailVerificationSpi.deleteAllExpiredTokenBefore(threshold);
        log.info("Deleted {} expired email verification tokens", deleted);
    }

}
