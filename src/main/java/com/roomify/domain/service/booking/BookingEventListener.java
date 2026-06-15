package com.roomify.domain.service.booking;

import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.roomify.domain.models.event.BookingCancelledEvent;
import com.roomify.domain.models.event.BookingConfirmedEvent;
import com.roomify.domain.models.event.BookingRequestedEvent;
import org.springframework.beans.factory.annotation.Value;

import com.roomify.domain.spi.EmailSenderSpi;
import com.roomify.shared.utils.EmailTemplateLoader;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BookingEventListener {

    private static final String TEMPLATE_REQUESTED_TENANT = "templates/email/booking-requested-tenant.html";
    private static final String TEMPLATE_REQUESTED_OWNER = "templates/email/booking-requested-owner.html";
    private static final String TEMPLATE_CONFIRMED = "templates/email/booking-confirmed.html";
    private static final String TEMPLATE_CANCELLED = "templates/email/booking-cancelled.html";
    private static final String FIRST_NAME = "firstName";
    private static final String PLACE_NAME = "placeName";
    private static final String PLACE_ADDRESS = "placeAddress";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String TOTAL_PRICE = "totalPrice";
    private static final String OWNER_FIRST_NAME = "ownerFirstName";
    private static final String TENANT_FIRST_NAME = "tenantFirstName";
    private static final String APP_URL = "appUrl";

    private final EmailSenderSpi emailSender;
    private final EmailTemplateLoader templateLoader;
    private final String frontendBaseUrl;

    public BookingEventListener(
            EmailSenderSpi emailSender,
            EmailTemplateLoader templateLoader,
            @Value("${frontend.base-url}") String frontendBaseUrl) {
        this.emailSender = emailSender;
        this.templateLoader = templateLoader;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingRequested(BookingRequestedEvent event) {
        log.info("Sending booking-requested emails for booking {}", event.bookingId());

        String tenantHtml = templateLoader.load(TEMPLATE_REQUESTED_TENANT, Map.of(
                FIRST_NAME, event.tenantFirstName(),
                PLACE_NAME, event.placeName(),
                PLACE_ADDRESS, event.placeAddress(),
                START_DATE, event.startDate().toString(),
                END_DATE, event.endDate().toString(),
                TOTAL_PRICE, event.totalPrice().toPlainString(),
                APP_URL, frontendBaseUrl
        ));
        emailSender.sendEmail(event.tenantEmail(), "Votre demande de réservation — Roomify", tenantHtml);

        String ownerHtml = templateLoader.load(TEMPLATE_REQUESTED_OWNER, Map.of(
                OWNER_FIRST_NAME, event.ownerFirstName(),
                TENANT_FIRST_NAME, event.tenantFirstName(),
                PLACE_NAME, event.placeName(),
                START_DATE, event.startDate().toString(),
                END_DATE, event.endDate().toString(),
                APP_URL, frontendBaseUrl
        ));
        emailSender.sendEmail(event.ownerEmail(), "Nouvelle demande de réservation — Roomify", ownerHtml);
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Sending booking-confirmed email to {}", event.tenantEmail());
        String html = templateLoader.load(TEMPLATE_CONFIRMED, Map.of(
                FIRST_NAME, event.tenantFirstName(),
                PLACE_NAME, event.placeName(),
                PLACE_ADDRESS, event.placeAddress(),
                START_DATE, event.startDate().toString(),
                END_DATE, event.endDate().toString(),
                TOTAL_PRICE, event.totalPrice().toPlainString(),
                APP_URL, frontendBaseUrl
        ));
        emailSender.sendEmail(event.tenantEmail(), "Réservation confirmée — Roomify", html);
    }

    @Async
    @Retry(name = "sendEmailRetry")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Sending booking-cancelled email to {}", event.tenantEmail());
        String html = templateLoader.load(TEMPLATE_CANCELLED, Map.of(
                FIRST_NAME, event.tenantFirstName(),
                PLACE_NAME, event.placeName(),
                START_DATE, event.startDate().toString(),
                END_DATE, event.endDate().toString(),
                APP_URL, frontendBaseUrl
        ));
        emailSender.sendEmail(event.tenantEmail(), "Réservation annulée — Roomify", html);
    }
}
