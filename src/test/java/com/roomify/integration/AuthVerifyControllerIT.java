package com.roomify.integration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.spi.EmailVerificationSpi;
import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.EmailVerificationToken;
import com.roomify.infrastucture.models.user.User;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import static com.roomify.integration.utils.UserUtils.createEmailVerificationToken;
import static com.roomify.integration.utils.UserUtils.createUser;
import static com.roomify.shared.utils.VerificationToken.sha256;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthVerifyControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationSpi emailVerificationSpi;

    @Autowired
    private UserSpi userSpi;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @BeforeEach
    void resetRateLimiter() {
        RateLimiterConfig config = rateLimiterRegistry
                .rateLimiter("creationalRateLimiter")
                .getRateLimiterConfig();

        rateLimiterRegistry.remove("creationalRateLimiter");
        rateLimiterRegistry.rateLimiter("creationalRateLimiter", config);
    }

    // =========================
    // ✅ NOMINAL
    // =========================
    @Test
    @Sql(statements = {
            "DELETE FROM roomify.email_verification_tokens WHERE user_id = (SELECT id FROM roomify.users WHERE email = 'verify.user@test.com')",
            "DELETE FROM roomify.user_roles WHERE user_id = (SELECT id FROM roomify.users WHERE email = 'verify.user@test.com')",
            "DELETE FROM roomify.users WHERE email = 'verify.user@test.com'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void verify_nominal_returns204_and_enable_user() throws Exception {
        String rawToken = "valid-token";
        String hashedToken = sha256(rawToken);

        User user = createUser("verify.user@test.com",
                "Verify",
                "Test",
                "password",
                false,
                false);
        userSpi.insertUser(user);
        EmailVerificationToken token = createEmailVerificationToken(hashedToken, user, LocalDateTime.now().plusMinutes(10));
        emailVerificationSpi.insertEmailVerification(token);

        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", rawToken))
                .andExpect(status().isNoContent());

        // Vérifie user activé
        User updated = userSpi.findUserByEmail("verify.user@test.com").orElseThrow();
        assertTrue(updated.isEnabled());
        assertTrue(updated.isEmailVerified());

        // Vérifie suppression du token
        assertTrue(emailVerificationSpi.findEmailVerification(hashedToken).isEmpty());
    }

    // =========================
    // ❌ TOKEN EXPIRED
    // =========================
    @Test
    @Sql(statements = {
            "DELETE FROM roomify.email_verification_tokens WHERE user_id = (SELECT id FROM roomify.users WHERE email = 'expired@test.com')",
            "DELETE FROM roomify.user_roles WHERE user_id = (SELECT id FROM roomify.users WHERE email = 'expired@test.com')",
            "DELETE FROM roomify.users WHERE email = 'expired@test.com'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void verify_expiredToken_returns400() throws Exception {
        String rawToken = "expired-token";
        String hashedToken = sha256(rawToken);

        User user = createUser(
                "expired@test.com",
                "Expired",
                "Test",
                "password",
                false,
                false);
        userSpi.insertUser(user);
        EmailVerificationToken token = createEmailVerificationToken(hashedToken, user, LocalDateTime.now().minusMinutes(1));
        emailVerificationSpi.insertEmailVerification(token);

        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", rawToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation email token expired"));
    }

    // =========================
    // ❌ TOKEN NOT FOUND
    // =========================
    @Test
    void verify_tokenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", "unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Validation email token not found"));
    }

    // =========================
    // ❌ ALREADY VERIFIED
    // =========================
    @Test
    @Sql(statements = {
            "DELETE FROM roomify.email_verification_tokens WHERE user_id = (SELECT id FROM roomify.users WHERE email = 'already@test.com')",
            "DELETE FROM roomify.user_roles WHERE user_id = (SELECT id FROM roomify.users WHERE email = 'already@test.com')",
            "DELETE FROM roomify.users WHERE email = 'already@test.com'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void verify_alreadyVerified_returns409() throws Exception {
        String rawToken = "already-used-token";
        String hashedToken = sha256(rawToken);

        User user = createUser(
                "already@test.com",
                "Already",
                "Test",
                "password",
                true,
                true);
        userSpi.insertUser(user);
        EmailVerificationToken token = createEmailVerificationToken(hashedToken, user, LocalDateTime.now().plusMinutes(10));
        emailVerificationSpi.insertEmailVerification(token);

        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", rawToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Account already verified. Please log in."));
    }

    // =========================
    // ❌ RATE LIMITER
    // =========================
    @Test
    void verify_rateLimiter_blocksTooManyRequest() throws Exception {
        String token = "rate-limit-token";

        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", token))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", token))
                .andExpect(status().isTooManyRequests());
    }
}
