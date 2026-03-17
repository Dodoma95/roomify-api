package com.roomify.integration;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.User;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import tools.jackson.databind.ObjectMapper;

import static com.roomify.integration.utils.UserUtils.createUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthLoginControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserSpi userSpi;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetRateLimiter() {
        RateLimiterConfig config = rateLimiterRegistry
                .rateLimiter("loginUserRateLimiter")
                .getRateLimiterConfig();

        rateLimiterRegistry.remove("loginUserRateLimiter");
        rateLimiterRegistry.rateLimiter("loginUserRateLimiter", config);
    }

    // =========================
    // ✅ NOMINAL
    // =========================
    @Test
    void login_nominal_returns200() throws Exception {
        String rawPassword = "Test@12345678941";
        User user = createUser("login@test.com", Objects.requireNonNull(passwordEncoder.encode(rawPassword)), true, true);
        userSpi.insertUser(user);
        var request = Map.of(
                "email", "login@test.com",
                "password", rawPassword
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    // =========================
    // ❌ BAD CREDENTIALS
    // =========================
    @Test
    void login_wrongPassword_returns404_or401() throws Exception {
        var request = Map.of(
                "email", "wrongpass@test.com",
                "password", "badPassword"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================
    // ❌ EMAIL NOT VERIFIED
    // =========================
    @Test
    void login_emailNotVerified_returns403() throws Exception {
        String rawPassword = "P@ssword123";
        User user = createUser("notverified@test.com", Objects.requireNonNull(passwordEncoder.encode(rawPassword)), true, false);
        userSpi.insertUser(user);
        var request = Map.of(
                "email", "notverified@test.com",
                "password", rawPassword
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================
    // ❌ RATE LIMITER
    // =========================
    @Test
    void login_rateLimiter_blocksTooManyRequest() throws Exception {
        var request = Map.of(
                "email", "ratelimit@test.com",
                "password", "whatever"
        );
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }

}
