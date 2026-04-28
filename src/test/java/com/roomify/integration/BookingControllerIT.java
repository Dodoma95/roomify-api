package com.roomify.integration;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Place de test ID 9000000011 : APPROVED, owner = user 99999999999 (admin)
 * Place de test ID 9000000012 : PENDING,  owner = user 99999999999 (admin)
 * Place de test ID 9000000013 : APPROVED, owner = user 99999999998 (user normal)
 */
@Sql(statements = {
        "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
        "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
        "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookingControllerIT extends AbstractIntegrationTest {

    private static final String ENDPOINT = "/api/v1/bookings";

    private static final LocalDate FUTURE_START = LocalDate.now().plusDays(10);
    private static final LocalDate FUTURE_END   = LocalDate.now().plusDays(15);

    @Autowired
    private MockMvc mockMvc;

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

    // =================================
    // ✅ NOMINAL
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createBooking_nominal_returns201() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9000000011L,
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.placeId").value(9000000011L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.startDate").value(FUTURE_START.toString()))
                .andExpect(jsonPath("$.endDate").value(FUTURE_END.toString()));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createBooking_withNotes_returns201() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9000000011L,
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString(),
                "notes", "Besoin d'un projecteur et d'un tableau blanc"
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").value("Besoin d'un projecteur et d'un tableau blanc"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id) VALUES (9000002001, 9000000011, '2099-01-10', '2099-01-15', 'BOOKING', 9000001001)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void cancelBooking_byBookingOwner_returns204() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/cancel")
                        .with(user(tenant)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id) VALUES (9000002001, 9000000011, '2099-01-10', '2099-01-15', 'BOOKING', 9000001001)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void cancelBooking_byAdmin_returns204() throws Exception {
        var admin = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.ADMIN).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/cancel")
                        .with(user(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getMyBookings_returns200WithList() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(get(ENDPOINT + "/me").with(user(tenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9000001001L))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getBookingsByPlace_byOwner_returns200() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(get(ENDPOINT + "/place/9000000011").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value(9000000011L));
    }

    // =================================
    // ❌ ERREURS DE VALIDATION
    // =================================

    @Test
    void createBooking_missingPlaceId_returns400() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_missingStartDate_returns400() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9000000011L,
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_pastStartDate_returns400() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9000000011L,
                "startDate", LocalDate.now().minusDays(1).toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_endDateNotFuture_returns400() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9000000011L,
                "startDate", FUTURE_START.toString(),
                "endDate", LocalDate.now().toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================================
    // ❌ ERREURS MÉTIER
    // =================================

    @Test
    void createBooking_placeNotFound_returns404() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9999999999L,
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000012, 'Salle Non Approuvee', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'PENDING', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createBooking_placeNotApproved_returns409() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        var request = Map.of(
                "placeId", 9000000012L,
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000013, 'Ma Salle Perso', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createBooking_ownPlace_returns403() throws Exception {
        var owner = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        var request = Map.of(
                "placeId", 9000000013L,
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id) VALUES (9000002001, 9000000011, '2099-01-10', '2099-01-15', 'BOOKING', 9000001001)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createBooking_overlappingDates_returns409() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        // Demande qui chevauche l'existant [2099-01-10, 2099-01-15]
        var request = Map.of(
                "placeId", 9000000011L,
                "startDate", "2099-01-12",
                "endDate", "2099-01-20"
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id) VALUES (9000002001, 9000000011, '2099-01-10', '2099-01-15', 'BOOKING', 9000001001)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void cancelBooking_alreadyCancelled_returns400() throws Exception {
        var admin = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.ADMIN).build()));

        // Premier cancel
        mockMvc.perform(patch(ENDPOINT + "/9000001001/cancel").with(user(admin)))
                .andExpect(status().isNoContent());

        // Deuxième cancel sur le même booking → 400
        mockMvc.perform(patch(ENDPOINT + "/9000001001/cancel").with(user(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBooking_bookingNotFound_returns404() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(patch(ENDPOINT + "/9999999999/cancel").with(user(tenant)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999999, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void cancelBooking_notOwnBooking_returns403() throws Exception {
        // user 99999999998 essaie d'annuler la réservation du user 99999999999
        var otherTenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/cancel").with(user(otherTenant)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getBookingsByPlace_notOwner_returns403() throws Exception {
        var otherUser = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(get(ENDPOINT + "/place/9000000011").with(user(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBookingsByPlace_placeNotFound_returns404() throws Exception {
        var admin = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.ADMIN).build()));

        mockMvc.perform(get(ENDPOINT + "/place/9999999999").with(user(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void confirmBooking_byOwner_returns200() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/confirm").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void confirmBooking_byAdmin_returns200() throws Exception {
        var admin = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.ADMIN).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/confirm").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void confirmBooking_alreadyConfirmed_returns400() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/confirm").with(user(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void confirmBooking_byTenant_returns403() throws Exception {
        // Le locataire ne peut pas confirmer sa propre réservation
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(patch(ENDPOINT + "/9000001001/confirm").with(user(tenant)))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmBooking_notFound_returns404() throws Exception {
        var admin = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.ADMIN).build()));

        mockMvc.perform(patch(ENDPOINT + "/9999999999/confirm").with(user(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001002, 99999999998, 9000000011, '2099-02-01', '2099-02-05', 'PENDING', 100.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getMyBookings_filteredByStatus_returns200() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        mockMvc.perform(get(ENDPOINT + "/me").param("status", "CONFIRMED").with(user(tenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    // =================================
    // ⚠️ RATE LIMITING
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000012, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000012, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Booking', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createBooking_rateLimitExceeded_returns429() throws Exception {
        var tenant = createCustomUserDetails(99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.USER).build()));

        // Épuise le rate limiter
        RateLimiterConfig config = rateLimiterRegistry.rateLimiter("creationalRateLimiter").getRateLimiterConfig();
        rateLimiterRegistry.remove("creationalRateLimiter");
        rateLimiterRegistry.rateLimiter("creationalRateLimiter",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(config.getLimitRefreshPeriod())
                        .timeoutDuration(config.getTimeoutDuration())
                        .build());

        var request = Map.of(
                "placeId", 9000000011L,
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );
        String body = new ObjectMapper().writeValueAsString(request);

        // Première requête consomme le seul appel disponible
        mockMvc.perform(post(ENDPOINT)
                .with(user(tenant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Deuxième requête → 429
        mockMvc.perform(post(ENDPOINT)
                        .with(user(tenant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }
}
