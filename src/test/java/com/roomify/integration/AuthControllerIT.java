package com.roomify.integration;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.roomify.domain.spi.EmailSenderSpi;
import com.roomify.domain.spi.UserSpi;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserSpi userSpi;

    @MockitoBean
    private EmailSenderSpi emailSender;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @BeforeEach
    void resetRateLimiter() {
        // récupère la config du RateLimiter existant
        RateLimiterConfig config = rateLimiterRegistry
                .rateLimiter("registerUserRateLimiter")
                .getRateLimiterConfig();

        // supprime l’ancien RateLimiter
        rateLimiterRegistry.remove("registerUserRateLimiter");

        // recrée un nouveau RateLimiter avec la même config
        rateLimiterRegistry.rateLimiter("registerUserRateLimiter", config);
    }

    @Test
    void register_nominal_returns201() throws Exception {
        var request = Map.of(
                "email", "new.user@example.com",
                "password", "P@ssword123567894"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully. Please verify your email before logging in."));

        // Vérifie que l'utilisateur est bien dans la base
        assertTrue(userSpi.alreadyExists("new.user@example.com"));

        // Vérifie que l'envoi du mail a été déclenché
        verify(emailSender, timeout(1000)).sendEmail(
                eq("new.user@example.com"),
                anyString(),
                contains("Click")
        );
    }

    @Test
    void register_emailAndPasswordValidationError() throws Exception {
        var request = Map.of(
                "email", "new.userexample.com",
                "password", "P@ssword123567894"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        var request2 = Map.of(
                "email", "new.user@example.com",
                "password", "P@ssword123"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request2)))
                .andExpect(status().isBadRequest());

        verify(emailSender, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void register_emailAlreadyExistsError() throws Exception {
        var request = Map.of(
                "email", "test.User@gmail.com",
                "password", "Test@12345678941"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid registration request"));

        verify(emailSender, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void register_rateLimiter_blocksTooManyRequest() throws Exception {
        var requestBody = Map.of(
                "email", "new.user2@example.com",
                "password", "P@ssword123567894"
        );
        var json = new ObjectMapper().writeValueAsString(requestBody);

        // Premier appel : ok
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Deuxième appel immédiat : devrait être bloqué par le rate limiter
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }

}
