package com.roomify.unit.service.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.roomify.domain.models.event.BookingCancelledEvent;
import com.roomify.domain.models.event.BookingConfirmedEvent;
import com.roomify.domain.models.event.BookingRequestedEvent;
import com.roomify.domain.service.booking.BookingEventListener;
import com.roomify.domain.spi.EmailSenderSpi;
import com.roomify.shared.utils.EmailTemplateLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventListenerTest {

    @Mock
    private EmailSenderSpi emailSender;

    @Mock
    private EmailTemplateLoader templateLoader;

    private BookingEventListener listener;

    private static final String FRONTEND_BASE_URL = "https://roomify.fr";

    @BeforeEach
    void setUp() {
        listener = new BookingEventListener(emailSender, templateLoader, FRONTEND_BASE_URL);
    }

    private static final LocalDate START = LocalDate.of(2099, 6, 1);
    private static final LocalDate END   = LocalDate.of(2099, 6, 5);

    // ─── handleBookingRequested ───────────────────────────────────────────────

    @Test
    void handleBookingRequested_sendsTwoEmails_tenantAndOwner() {
        when(templateLoader.load(anyString(), anyMap())).thenReturn("<html>content</html>");

        listener.handleBookingRequested(new BookingRequestedEvent(
                42L,
                "tenant@example.com", "Alice",
                "owner@example.com", "Bob",
                "Salle Paris", "10 rue de Paris",
                START, END,
                BigDecimal.valueOf(125)
        ));

        verify(emailSender, times(2)).sendEmail(anyString(), anyString(), anyString());
        verify(emailSender).sendEmail(eq("tenant@example.com"), eq("Votre demande de réservation — Roomify"), anyString());
        verify(emailSender).sendEmail(eq("owner@example.com"), eq("Nouvelle demande de réservation — Roomify"), anyString());
    }

    @Test
    void handleBookingRequested_passesTenantVariablesToTemplate() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        when(templateLoader.load(anyString(), anyMap())).thenReturn("html");

        listener.handleBookingRequested(new BookingRequestedEvent(
                1L, "t@x.com", "Alice", "o@x.com", "Bob",
                "Mon Espace", "5 rue Lyon", START, END, BigDecimal.valueOf(75)
        ));

        verify(templateLoader, times(2)).load(anyString(), captor.capture());
        Map<String, String> tenantVars = captor.getAllValues().getFirst();
        assertThat(tenantVars)
                .containsEntry("firstName", "Alice")
                .containsEntry("placeName", "Mon Espace")
                .containsEntry("placeAddress", "5 rue Lyon")
                .containsEntry("startDate", "2099-06-01")
                .containsEntry("endDate", "2099-06-05")
                .containsEntry("totalPrice", "75")
                .containsEntry("appUrl", FRONTEND_BASE_URL);
    }

    @Test
    void handleBookingRequested_passesOwnerVariablesToTemplate() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        when(templateLoader.load(anyString(), anyMap())).thenReturn("html");

        listener.handleBookingRequested(new BookingRequestedEvent(
                1L, "t@x.com", "Alice", "o@x.com", "Bob",
                "Mon Espace", "5 rue Lyon", START, END, BigDecimal.valueOf(75)
        ));

        verify(templateLoader, times(2)).load(anyString(), captor.capture());
        Map<String, String> ownerVars = captor.getAllValues().get(1);
        assertThat(ownerVars)
                .containsEntry("ownerFirstName", "Bob")
                .containsEntry("tenantFirstName", "Alice")
                .containsEntry("placeName", "Mon Espace")
                .containsEntry("appUrl", FRONTEND_BASE_URL);
    }

    // ─── handleBookingConfirmed ───────────────────────────────────────────────

    @Test
    void handleBookingConfirmed_sendsEmailToTenant() {
        when(templateLoader.load(anyString(), anyMap())).thenReturn("<html>confirmed</html>");

        listener.handleBookingConfirmed(new BookingConfirmedEvent(
                "tenant@example.com", "Alice",
                "Salle Paris", "10 rue de Paris",
                START, END, BigDecimal.valueOf(125)
        ));

        verify(emailSender).sendEmail(
                "tenant@example.com",
                "Réservation confirmée — Roomify",
                "<html>confirmed</html>"
        );
    }

    @Test
    void handleBookingConfirmed_passesCorrectVariables() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        when(templateLoader.load(anyString(), captor.capture())).thenReturn("html");

        listener.handleBookingConfirmed(new BookingConfirmedEvent(
                "t@x.com", "Alice", "Salle A", "1 rue A", START, END, BigDecimal.valueOf(200)
        ));

        Map<String, String> vars = captor.getValue();
        assertThat(vars)
                .containsEntry("firstName", "Alice")
                .containsEntry("placeName", "Salle A")
                .containsEntry("placeAddress", "1 rue A")
                .containsEntry("startDate", "2099-06-01")
                .containsEntry("endDate", "2099-06-05")
                .containsEntry("totalPrice", "200")
                .containsEntry("appUrl", FRONTEND_BASE_URL);
    }

    // ─── handleBookingCancelled ───────────────────────────────────────────────

    @Test
    void handleBookingCancelled_sendsEmailToTenant() {
        when(templateLoader.load(anyString(), anyMap())).thenReturn("<html>cancelled</html>");

        listener.handleBookingCancelled(new BookingCancelledEvent(
                "tenant@example.com", "Alice", "Salle Paris", START, END
        ));

        verify(emailSender).sendEmail(
                "tenant@example.com",
                "Réservation annulée — Roomify",
                "<html>cancelled</html>"
        );
    }

    @Test
    void handleBookingCancelled_passesCorrectVariables() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        when(templateLoader.load(anyString(), captor.capture())).thenReturn("html");

        listener.handleBookingCancelled(new BookingCancelledEvent(
                "t@x.com", "Bob", "Salle B", START, END
        ));

        Map<String, String> vars = captor.getValue();
        assertThat(vars)
                .containsEntry("firstName", "Bob")
                .containsEntry("placeName", "Salle B")
                .containsEntry("startDate", "2099-06-01")
                .containsEntry("endDate", "2099-06-05")
                .containsEntry("appUrl", FRONTEND_BASE_URL);
    }
}
