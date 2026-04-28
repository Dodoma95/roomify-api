package com.roomify.integration;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Place de test ID 9000000011 : APPROVED, owner = user 99999999999 (admin)
 * Place de test ID 9000000013 : APPROVED, owner = user 99999999998 (user normal)
 */
@Sql(statements = {
        "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
        "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
        "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlaceUnavailabilityControllerIT extends AbstractIntegrationTest {

    private static final String ENDPOINT = "/api/v1/places/{placeId}/unavailability";

    private static final LocalDate FUTURE_START = LocalDate.now().plusDays(10);
    private static final LocalDate FUTURE_END   = LocalDate.now().plusDays(15);

    @Autowired
    private MockMvc mockMvc;

    // =================================
    // ✅ NOMINAL
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason) VALUES (9000002001, 9000000011, '2099-06-01', '2099-06-05', 'OWNER_BLOCKED')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getPlaceUnavailability_nominal_returns200WithList() throws Exception {
        var user = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(get(ENDPOINT, 9000000011L).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9000002001L))
                .andExpect(jsonPath("$[0].placeId").value(9000000011L))
                .andExpect(jsonPath("$[0].reason").value("OWNER_BLOCKED"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void blockDates_byOwner_returns201() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        var request = Map.of(
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT + "/block", 9000000011L)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.placeId").value(9000000011L))
                .andExpect(jsonPath("$.reason").value("OWNER_BLOCKED"))
                .andExpect(jsonPath("$.startDate").value(FUTURE_START.toString()))
                .andExpect(jsonPath("$.endDate").value(FUTURE_END.toString()));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason) VALUES (9000002001, 9000000011, '2099-06-01', '2099-06-05', 'OWNER_BLOCKED')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void unblockDates_byOwner_returns204() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(delete(ENDPOINT + "/9000002001", 9000000011L).with(user(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void blockDates_byAdmin_returns201() throws Exception {
        var admin = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.ADMIN).build()));

        var request = Map.of(
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT + "/block", 9000000011L)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // =================================
    // ❌ ERREURS DE VALIDATION
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void blockDates_missingStartDate_returns400() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        var request = Map.of("endDate", FUTURE_END.toString());

        mockMvc.perform(post(ENDPOINT + "/block", 9000000011L)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void blockDates_pastStartDate_returns400() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        var request = Map.of(
                "startDate", LocalDate.now().minusDays(1).toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT + "/block", 9000000011L)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================================
    // ❌ ERREURS MÉTIER
    // =================================

    @Test
    void getPlaceUnavailability_placeNotFound_returns404() throws Exception {
        var user = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(get(ENDPOINT, 9999999999L).with(user(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000013, 'Salle autre owner', '5 rue test 75001 paris', '5 rue test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void blockDates_notOwner_returns403() throws Exception {
        // user 99999999999 essaie de bloquer la place de user 99999999998
        var otherUser = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        var request = Map.of(
                "startDate", FUTURE_START.toString(),
                "endDate", FUTURE_END.toString()
        );

        mockMvc.perform(post(ENDPOINT + "/block", 9000000013L)
                        .with(user(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason) VALUES (9000002001, 9000000011, '2099-06-10', '2099-06-15', 'OWNER_BLOCKED')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void blockDates_overlappingExistingBlock_returns409() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        var request = Map.of(
                "startDate", "2099-06-12",
                "endDate", "2099-06-20"
        );

        mockMvc.perform(post(ENDPOINT + "/block", 9000000011L)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000001001, 99999999998, 9000000011, '2099-01-10', '2099-01-15', 'CONFIRMED', 125.00)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id) VALUES (9000002001, 9000000011, '2099-01-10', '2099-01-15', 'BOOKING', 9000001001)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void unblockDates_bookingLinkedEntry_returns403() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        // Tentative de supprimer une entrée liée à un booking → interdit
        mockMvc.perform(delete(ENDPOINT + "/9000002001", 9000000011L).with(user(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.bookings             WHERE place_id IN (9000000011, 9000000013)",
            "DELETE FROM roomify.places               WHERE id       IN (9000000011, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000011, 'Salle Test Unavail', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void unblockDates_notFound_returns404() throws Exception {
        var owner = createCustomUserDetails(99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941", Set.of(Role.builder().name(RoleEnum.OWNER).build()));

        mockMvc.perform(delete(ENDPOINT + "/9999999999", 9000000011L).with(user(owner)))
                .andExpect(status().isNotFound());
    }
}
