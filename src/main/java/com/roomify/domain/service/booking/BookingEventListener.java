package com.roomify.domain.service.booking;

import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.roomify.domain.models.event.BookingCancelledEvent;
import com.roomify.domain.models.event.BookingConfirmedEvent;
import com.roomify.domain.models.event.BookingRequestedEvent;
import com.roomify.domain.spi.EmailSenderSpi;
import com.roomify.shared.utils.EmailTemplateLoader;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BookingEventListener {

    private static final String TEMPLATE_REQUESTED_TENANT = "templates/email/booking-requested-tenant.html";
    private static final String TEMPLATE_REQUESTED_OWNER  = "templates/email/booking-requested-owner.html";
    private static final String TEMPLATE_CONFIRMED        = "templates/email/booking-confirmed.html";
    private static final String TEMPLATE_CANCELLED        = "templates/email/booking-cancelled.html";

    private final EmailSenderSpi emailSender;
    private final EmailTemplateLoader templateLoader;

    public BookingEventListener(EmailSenderSpi emailSender, EmailTemplateLoader templateLoader) {
        this.emailSender = emailSender;
        this.templateLoader = templateLoader;
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingRequested(BookingRequestedEvent event) {
        log.info("Sending booking-requested emails for booking {}", event.bookingId());

        String tenantHtml = templateLoader.load(TEMPLATE_REQUESTED_TENANT, Map.of(
                "firstName", event.tenantFirstName(),
                "placeName", event.placeName(),
                "placeAddress", event.placeAddress(),
                "startDate", event.startDate().toString(),
                "endDate", event.endDate().toString(),
                "totalPrice", event.totalPrice().toPlainString()
        ));
        emailSender.sendEmail(event.tenantEmail(), "Votre demande de réservation — Roomify", tenantHtml);

        String ownerHtml = templateLoader.load(TEMPLATE_REQUESTED_OWNER, Map.of(
                "ownerFirstName", event.ownerFirstName(),
                "tenantFirstName", event.tenantFirstName(),
                "placeName", event.placeName(),
                "startDate", event.startDate().toString(),
                "endDate", event.endDate().toString()
        ));
        emailSender.sendEmail(event.ownerEmail(), "Nouvelle demande de réservation — Roomify", ownerHtml);
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Sending booking-confirmed email to {}", event.tenantEmail());
        String html = templateLoader.load(TEMPLATE_CONFIRMED, Map.of(
                "firstName", event.tenantFirstName(),
                "placeName", event.placeName(),
                "placeAddress", event.placeAddress(),
                "startDate", event.startDate().toString(),
                "endDate", event.endDate().toString(),
                "totalPrice", event.totalPrice().toPlainString()
        ));
        emailSender.sendEmail(event.tenantEmail(), "Réservation confirmée — Roomify", html);
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Sending booking-cancelled email to {}", event.tenantEmail());
        String html = templateLoader.load(TEMPLATE_CANCELLED, Map.of(
                "firstName", event.tenantFirstName(),
                "placeName", event.placeName(),
                "startDate", event.startDate().toString(),
                "endDate", event.endDate().toString()
        ));
        emailSender.sendEmail(event.tenantEmail(), "Réservation annulée — Roomify", html);
    }
}
