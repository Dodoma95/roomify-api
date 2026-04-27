package com.roomify.unit.service.auth;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.roomify.domain.models.event.UserRegisteredEvent;
import com.roomify.domain.service.auth.UserRegisteredListener;
import com.roomify.domain.spi.EmailSenderSpi;
import com.roomify.shared.utils.EmailTemplateLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredListenerTest {

    @Mock
    private EmailSenderSpi emailSender;

    @Mock
    private EmailTemplateLoader templateLoader;

    private UserRegisteredListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserRegisteredListener(
                "http://localhost:8080",
                "/api/v1/auth/verify",
                emailSender,
                templateLoader
        );
    }

    @Test
    void handle_sendsEmailWithRenderedTemplate() {
        when(templateLoader.load(eq("templates/email/user-registration.html"), anyMap()))
                .thenReturn("<html>Confirmer mon email</html>");

        listener.handle(new UserRegisteredEvent(1L, "user@example.com", "Alice", "raw-token-abc"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateLoader).load(eq("templates/email/user-registration.html"), varsCaptor.capture());
        Map<String, String> vars = varsCaptor.getValue();
        assertThat(vars).containsEntry("firstName", "Alice");
        assertThat(vars.get("verificationLink")).contains("raw-token-abc");

        verify(emailSender).sendEmail(
                eq("user@example.com"),
                eq("Confirmez votre compte Roomify"),
                eq("<html>Confirmer mon email</html>")
        );
    }

    @Test
    void handle_buildsVerificationLinkWithToken() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        when(templateLoader.load(eq("templates/email/user-registration.html"), anyMap())).thenReturn("html");

        listener.handle(new UserRegisteredEvent(1L, "user@example.com", "Bob", "my-secret-token"));

        verify(templateLoader).load(eq("templates/email/user-registration.html"), varsCaptor.capture());
        String link = varsCaptor.getValue().get("verificationLink");
        assertThat(link)
                .contains("http://localhost:8080")
                .contains("/api/v1/auth/verify")
                .contains("token=my-secret-token");
    }
}
