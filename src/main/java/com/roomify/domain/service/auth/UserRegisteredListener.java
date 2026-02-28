package com.roomify.domain.service.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.roomify.domain.models.event.UserRegisteredEvent;
import com.roomify.domain.spi.EmailSenderSpi;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

import static com.roomify.shared.utils.UriBuilder.build;
import static org.springframework.util.MultiValueMap.fromSingleValue;

@Component
@Slf4j
public class UserRegisteredListener {

    private final String baseUrl;
    private final String basePath;
    private final EmailSenderSpi emailSender;

    public UserRegisteredListener(
            @Value("${api.base-url}") String baseUrl,
            @Value("${api.endpoints.verify-user-path}") String basePath,
            EmailSenderSpi emailSender) {
        this.baseUrl = baseUrl;
        this.basePath = basePath;
        this.emailSender = emailSender;
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserRegisteredEvent event) {
        log.info("Processing email verification for {}", event.email());
        String link = build(baseUrl, basePath, fromSingleValue(Map.of("token", event.token())));

        // TODO revoir template du mail OU fabriquer template brevo
        emailSender.sendEmail(
                event.email(),
                "Confirm your account",
                "<h1>Welcome</h1>" +
                "<p>Click <a href=\"" + link + "\">here</a></p>"
        );
    }

}
