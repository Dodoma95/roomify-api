package com.roomify.infrastucture.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.roomify.infrastucture.models.user.EmailVerificationToken;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String hashedToken);

    @Modifying
    @Query("""
                DELETE FROM EmailVerificationToken t
                WHERE t.expiresAt < :threshold
            """)
    int deleteAllExpiredBefore(LocalDateTime threshold);

}
