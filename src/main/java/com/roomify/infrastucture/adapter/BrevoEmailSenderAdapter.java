package com.roomify.infrastucture.adapter;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.roomify.configuration.brevo.BrevoProperties;
import com.roomify.domain.spi.EmailSenderSpi;
import com.roomify.infrastucture.models.brevo.BrevoErrorResponse;
import com.roomify.shared.exception.ClientApiException;
import com.roomify.shared.exception.TechniqueApiException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class BrevoEmailSenderAdapter implements EmailSenderSpi {

    private final WebClient brevoWebClient;
    private final BrevoProperties properties;

    public BrevoEmailSenderAdapter(WebClient brevoWebClient, BrevoProperties properties) {
        this.brevoWebClient = brevoWebClient;
        this.properties = properties;
    }

    @Override
    public void sendEmail(
            @NonNull String to,
            @NonNull String subject,
            @NonNull String content) {

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "email", properties.senderEmail(),
                        "name", properties.senderName()
                ),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", content
        );

        try {
            brevoWebClient.post()
                    .uri(properties.brevoSmtpPath())
                    .header("api-key", properties.apiKey())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            this::handleBrevo4xx
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            this::handleBrevo5xx
                    )
                    .toBodilessEntity()
                    .block();

            log.info("Email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {} : {}", to, e.getMessage());
            throw new TechniqueApiException("Une erreur technique est survenue lors de l'envoi de l'email de validation du compte", e);
        }
    }

    private Mono<? extends Throwable> handleBrevo4xx(ClientResponse response) {
        return response.bodyToMono(BrevoErrorResponse.class)
                .flatMap(error ->
                        Mono.error(ClientApiException.ofBadRequest(error.message(), null))
                );
    }

    private Mono<? extends Throwable> handleBrevo5xx(ClientResponse response) {
        return response.bodyToMono(BrevoErrorResponse.class)
                .defaultIfEmpty(new BrevoErrorResponse(
                        "unknown",
                        "No response body"
                ))
                .flatMap(error ->
                        Mono.error(new TechniqueApiException("Brevo server error: " + error.message(), null))
                );
    }
}
