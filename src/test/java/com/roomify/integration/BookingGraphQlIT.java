package com.roomify.integration;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.CustomUserDetails;
import com.roomify.infrastucture.models.user.Role;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Place 9000000021 : MEETING_ROOM,    APPROVED, owner=99999999999 (Test Admin), pricePerHour=25.00
 * Place 9000000022 : COWORKING_SPACE, APPROVED, owner=99999999998 (Test User),  pricePerHour=50.00
 * Booking 9000003001 : user=99999999998, place=9000000021, CONFIRMED, 100.00, notes="Besoin projecteur"
 * Booking 9000003002 : user=99999999999, place=9000000021, PENDING,   125.00, sans notes
 * Booking 9000003003 : user=99999999998, place=9000000022, CANCELLED, 200.00, sans notes
 */
@SuppressWarnings("java:S5976")
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(statements = {
        "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
        "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookingGraphQlIT extends AbstractIntegrationTest {

    private static final String GRAPHQL_ENDPOINT = "/graphql";

    @Autowired
    private MockMvc mockMvc;

    // ==================================================
    // ✅ NOMINAL — sans filtre
    // ==================================================

    @Test
    void bookings_noFilter_returnsResults() throws Exception {
        String query = """
                { "query": "{ bookings { results { id status } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.results", notNullValue()))
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements", notNullValue()));
    }

    // ==================================================
    // ✅ NOMINAL — filtre statuses
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByStatus_returnsOnlyMatchingStatuses() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { statuses: [CONFIRMED], placeId: \\"9000000021\\" }) { results { id status } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].status").value("CONFIRMED"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre placeId
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000022, 'Espace Coworking GraphQL', '20 rue de lyon 75012 paris', '20 rue de Lyon, 75012 Paris', 'COWORKING_SPACE', 'APPROVED', 50.00, 99999999998)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003003, 99999999998, 9000000022, '2099-08-01', '2099-08-05', 'CANCELLED', 200.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByPlaceId_returnsOnlyBookingsForThatPlace() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { placeId: \\"9000000021\\" }) { results { id place { id } } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.id").value("9000000021"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre userId (locataire)
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByUserId_returnsOnlyBookingsForThatUser() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { userId: \\"99999999998\\", placeId: \\"9000000021\\" }) { results { id booker { id } } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].booker.id").value("99999999998"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre ownerId
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000022, 'Espace Coworking GraphQL', '20 rue de lyon 75012 paris', '20 rue de Lyon, 75012 Paris', 'COWORKING_SPACE', 'APPROVED', 50.00, 99999999998)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003003, 99999999998, 9000000022, '2099-08-01', '2099-08-05', 'CANCELLED', 200.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByOwnerId_returnsOnlyBookingsForPlacesOwnedByThatUser() throws Exception {
        // ownerId=99999999999 → seul le lieu 9000000021 lui appartient dans les données de test
        String query = """
                { "query": "{ bookings(filter: { ownerId: \\"99999999999\\", placeId: \\"9000000021\\" }) { results { id place { owner { id } } } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.owner.id").value("99999999999"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre startDateFrom
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByStartDateFrom_returnsOnlyBookingsFromThatDate() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { startDateFrom: \\"2099-07-01\\", placeId: \\"9000000021\\" }) { results { id startDate } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003002"))
                .andExpect(jsonPath("$.data.bookings.results[0].startDate").value("2099-07-10"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre endDateTo
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByEndDateTo_returnsOnlyBookingsEndingBeforeThatDate() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { endDateTo: \\"2099-06-10\\", placeId: \\"9000000021\\" }) { results { id endDate } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].endDate").value("2099-06-05"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre totalPriceMin / totalPriceMax
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByPriceRange_returnsOnlyBookingsInThatPriceRange() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { totalPriceMin: 110.0, totalPriceMax: 150.0, placeId: \\"9000000021\\" }) { results { id totalPrice } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003002"))
                .andExpect(jsonPath("$.data.bookings.results[0].totalPrice").value(125.0));
    }

    // ==================================================
    // ✅ NOMINAL — filtre placeTypes
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000022, 'Espace Coworking GraphQL', '20 rue de lyon 75012 paris', '20 rue de Lyon, 75012 Paris', 'COWORKING_SPACE', 'APPROVED', 50.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003003, 99999999998, 9000000022, '2099-08-01', '2099-08-05', 'CANCELLED', 200.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByPlaceType_returnsOnlyBookingsForThatPlaceType() throws Exception {
        // ownerId=99999999999 restreint aux deux lieux de test, placeTypes=[COWORKING_SPACE] → seul 9000003003
        String query = """
                { "query": "{ bookings(filter: { placeTypes: [COWORKING_SPACE], ownerId: \\"99999999999\\" }) { results { id place { id type } } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003003"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.type").value("COWORKING_SPACE"));
    }

    // ==================================================
    // ✅ NOMINAL — filtre notesContains
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_filterByNotesContains_returnsOnlyBookingsWithMatchingNotes() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { notesContains: \\"PROJECTEUR\\", placeId: \\"9000000021\\" }) { results { id notes } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].notes").value("Besoin projecteur"));
    }

    // ==================================================
    // ✅ NOMINAL — pagination
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003002, 99999999999, 9000000021, '2099-07-10', '2099-07-15', 'PENDING', 125.00)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000003004, 99999999998, 9000000021, '2099-09-01', '2099-09-05', 'CONFIRMED', 75.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_pagination_returnsCorrectPageMetadata() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { placeId: \\"9000000021\\" }, pagination: { page: 0, pageSize: 2 }) { results { id } pageInfo { page pageSize totalElements totalPages hasNext hasPrevious } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.results", hasSize(2)))
                .andExpect(jsonPath("$.data.bookings.pageInfo.page").value(0))
                .andExpect(jsonPath("$.data.bookings.pageInfo.pageSize").value(2))
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalElements").value(3))
                .andExpect(jsonPath("$.data.bookings.pageInfo.totalPages").value(2))
                .andExpect(jsonPath("$.data.bookings.pageInfo.hasNext").value(true))
                .andExpect(jsonPath("$.data.bookings.pageInfo.hasPrevious").value(false));
    }

    // ==================================================
    // ✅ NOMINAL — champs de réponse complets
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000021, 9000000022)",
            "DELETE FROM roomify.places   WHERE id       IN (9000000021, 9000000022)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000021, 'Salle GraphQL Test', '10 rue de paris 75001 paris', '10 rue de Paris, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 99999999999)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes) VALUES (9000003001, 99999999998, 9000000021, '2099-06-01', '2099-06-05', 'CONFIRMED', 100.00, 'Besoin projecteur')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void bookings_fullResponse_allFieldsPresent() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { placeId: \\"9000000021\\" }) { results { id status totalPrice startDate endDate notes createdAt place { id name address type status pricePerHour owner { id firstName lastName email } } booker { id firstName lastName email } } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.bookings.results", hasSize(1)))
                .andExpect(jsonPath("$.data.bookings.results[0].id").value("9000003001"))
                .andExpect(jsonPath("$.data.bookings.results[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.bookings.results[0].totalPrice").value(100.0))
                .andExpect(jsonPath("$.data.bookings.results[0].startDate").value("2099-06-01"))
                .andExpect(jsonPath("$.data.bookings.results[0].endDate").value("2099-06-05"))
                .andExpect(jsonPath("$.data.bookings.results[0].notes").value("Besoin projecteur"))
                .andExpect(jsonPath("$.data.bookings.results[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.bookings.results[0].place.id").value("9000000021"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.name").value("Salle GraphQL Test"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.address").value("10 rue de Paris, 75001 Paris"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.type").value("MEETING_ROOM"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.pricePerHour").value(25.0))
                .andExpect(jsonPath("$.data.bookings.results[0].place.owner.id").value("99999999999"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.owner.firstName").value("Test"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.owner.lastName").value("Admin"))
                .andExpect(jsonPath("$.data.bookings.results[0].place.owner.email").value("test.admin@gmail.com"))
                .andExpect(jsonPath("$.data.bookings.results[0].booker.id").value("99999999998"))
                .andExpect(jsonPath("$.data.bookings.results[0].booker.firstName").value("Test"))
                .andExpect(jsonPath("$.data.bookings.results[0].booker.lastName").value("User"))
                .andExpect(jsonPath("$.data.bookings.results[0].booker.email").value("test.user@gmail.com"));
    }

    // ==================================================
    // ❌ ERREURS DE VALIDATION
    // ==================================================

    @Test
    void bookings_pageNegative_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(pagination: { page: -1, pageSize: 10 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_pageSizeZero_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(pagination: { page: 0, pageSize: 0 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_pageSizeExceedsMax_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(pagination: { page: 0, pageSize: 51 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_startDateRangeIncoherent_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { startDateFrom: \\"2099-07-10\\", startDateTo: \\"2099-06-01\\" }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_endDateRangeIncoherent_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { endDateFrom: \\"2099-07-15\\", endDateTo: \\"2099-06-05\\" }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_priceRangeIncoherent_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { totalPriceMin: 200.0, totalPriceMax: 100.0 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_negativeTotalPriceMin_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { totalPriceMin: -1.0 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void bookings_createdAtRangeIncoherent_returnsBadRequestError() throws Exception {
        String query = """
                { "query": "{ bookings(filter: { createdAtFrom: \\"2025-12-31T00:00:00Z\\", createdAtTo: \\"2025-01-01T00:00:00Z\\" }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    // ==================================================
    // 🔒 SÉCURITÉ
    // ==================================================

    @Test
    void bookings_unauthenticated_returnsForbiddenError() throws Exception {
        String query = """
                { "query": "{ bookings { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    void bookings_roleUser_returnsForbiddenError() throws Exception {
        var regularUser = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ bookings { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(regularUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    void bookings_roleOwner_returnsForbiddenError() throws Exception {
        var owner = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        String query = """
                { "query": "{ bookings { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static CustomUserDetails admin() {
        return createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
    }
}
