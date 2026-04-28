package com.roomify.integration;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("java:S5976") // Suppression du warning "replace with a single Parameterized one" non souhaité ici
@Sql(statements = {
        "DELETE FROM roomify.place_unavailability WHERE place_id IN (SELECT id FROM roomify.places WHERE user_id IN (99999999998, 99999999999))",
        "DELETE FROM roomify.bookings WHERE place_id IN (SELECT id FROM roomify.places WHERE user_id IN (99999999998, 99999999999))",
        "DELETE FROM roomify.places WHERE user_id IN (99999999998, 99999999999)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlaceGraphQlIT extends AbstractIntegrationTest {

    private static final String GRAPHQL_ENDPOINT = "/graphql";

    // Dates fixes dans le futur — 2030-06 est toujours valide quel que soit le jour d'exécution
    private static final String FUTURE_FROM = "2030-06-01";
    private static final String FUTURE_TO   = "2030-06-10";  // 9 jours < 30 jours max

    @Autowired
    private MockMvc mockMvc;

    // ==================================================
    // ✅ NOMINAL — recherche générale
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Salle GraphQL Alpha', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 10, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Studio GraphQL Beta', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 50.00, 5, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000032, 'Espace GraphQL Gamma', '3 rue test 75001 paris', '3 rue Test, 75001 Paris', 'EVENT_SPACE', 'PENDING', 120.00, 100, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_noFilter_returnsAllPlaces() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places { results { id name type status } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places.results", notNullValue()))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements", notNullValue()))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Salle GraphQL Alpha', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 10, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Studio GraphQL Beta', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 50.00, 5, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000032, 'Espace GraphQL Gamma', '3 rue test 75001 paris', '3 rue Test, 75001 Paris', 'EVENT_SPACE', 'PENDING', 120.00, 100, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByType_returnsMatchingPlaces() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { types: [MEETING_ROOM] }) { results { id name type } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results[*].type").value(
                        everyItem(is("MEETING_ROOM"))
                ));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Salle GraphQL Alpha', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 10, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Studio GraphQL Beta', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 50.00, 5, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000032, 'Espace GraphQL Gamma', '3 rue test 75001 paris', '3 rue Test, 75001 Paris', 'EVENT_SPACE', 'PENDING', 120.00, 100, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByStatus_returnsMatchingPlaces() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { statuses: [PENDING] }) { results { id name status } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results[*].status").value(
                        everyItem(is("PENDING"))
                ));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Salle GraphQL Alpha', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 10, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Studio GraphQL Beta', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 50.00, 5, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByOwnerId_returnsOnlyThatOwnersPlaces() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\" }) { results { id name } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(2)))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(2));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Petite Salle', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 15.00, 5, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Salle Moyenne', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 40.00, 20, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000032, 'Grande Salle', '3 rue test 75001 paris', '3 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 80.00, 50, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByPriceRange_returnsMatchingPlaces() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\", pricePerHourMin: 20, pricePerHourMax: 60 }) { results { id name pricePerHour } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(1)))
                .andExpect(jsonPath("$.data.places.results[0].name").value("Salle Moyenne"))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(1));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Salle de Brainstorming', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 10, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Studio Photo Pro', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 50.00, 5, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000032, 'Grande Salle Evenement', '3 rue test 75001 paris', '3 rue Test, 75001 Paris', 'EVENT_SPACE', 'APPROVED', 120.00, 100, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByNameContains_returnsMatchingPlaces() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\", nameContains: \\"Salle\\" }) { results { id name } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(2)))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(2));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000030, 'Place Pagination 1', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 10.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000031, 'Place Pagination 2', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 11.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000032, 'Place Pagination 3', '3 rue test 75001 paris', '3 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 12.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000033, 'Place Pagination 4', '4 rue test 75001 paris', '4 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 13.00, 99999999999)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000034, 'Place Pagination 5', '5 rue test 75001 paris', '5 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 14.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_pagination_respectsPageSize() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999999\\", types: [STUDIO] }, pagination: { pageSize: 2 }) { results { id name } pageInfo { totalElements pageSize hasNext hasPrevious } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(2)))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(5))
                .andExpect(jsonPath("$.data.places.pageInfo.pageSize").value(2))
                .andExpect(jsonPath("$.data.places.pageInfo.hasNext").value(true))
                .andExpect(jsonPath("$.data.places.pageInfo.hasPrevious").value(false));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000030, 'Salle Owner Alpha', '1 rue test 75001 paris', '1 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 25.00, 10, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000031, 'Studio Owner Beta', '2 rue test 75001 paris', '2 rue Test, 75001 Paris', 'STUDIO', 'APPROVED', 50.00, 5, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_withOwnerField_returnsOwnerInfo() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\" }) { results { id name owner { id firstName lastName email } } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(2)))
                .andExpect(jsonPath("$.data.places.results[0].owner.firstName").value("Test"))
                .andExpect(jsonPath("$.data.places.results[0].owner.lastName").value("User"))
                .andExpect(jsonPath("$.data.places.results[0].owner.email").value("test.user@gmail.com"))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(2));
    }

    // ==================================================
    // ✅ NOMINAL — filtre de disponibilité (search)
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle Dispo Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByAvailability_noConflict_includesPlace() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\", availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id name } pageInfo { totalElements } } }" }
                """.formatted(FUTURE_FROM, FUTURE_TO);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(1)))
                .andExpect(jsonPath("$.data.places.results[0].id").value("9000000035"))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(1));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle Reservee Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000000060, 99999999999, 9000000035, '2030-06-03', '2030-06-07', 'CONFIRMED', 90.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByAvailability_confirmedBookingOverlaps_excludesPlace() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // [2030-06-01, 2030-06-10] chevauche la réservation [2030-06-03, 2030-06-07] → place exclue
        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\", availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id name } pageInfo { totalElements } } }" }
                """.formatted(FUTURE_FROM, FUTURE_TO);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(0)))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(0));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034, 9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034, 9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034, 9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle Bloquee Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason) VALUES (9000000061, 9000000035, '2030-06-05', '2030-06-08', 'OWNER_BLOCKED')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByAvailability_ownerBlockedOverlaps_excludesPlace() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // [2030-06-01, 2030-06-10] chevauche OWNER_BLOCKED [2030-06-05, 2030-06-08] → place exclue
        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\", availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id name } pageInfo { totalElements } } }" }
                """.formatted(FUTURE_FROM, FUTURE_TO);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(0)))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(0));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034, 9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034, 9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000030, 9000000031, 9000000032, 9000000033, 9000000034, 9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle Dispo Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)",
            "INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price) VALUES (9000000060, 99999999999, 9000000035, '2030-06-03', '2030-06-07', 'CANCELLED', 90.00)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_filterByAvailability_cancelledBookingOverlaps_includesPlace() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // Réservation CANCELLED → n'est pas dans place_unavailability → place incluse
        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\", availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id name } pageInfo { totalElements } } }" }
                """.formatted(FUTURE_FROM, FUTURE_TO);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results", hasSize(1)))
                .andExpect(jsonPath("$.data.places.pageInfo.totalElements").value(1));
    }

    // ==================================================
    // ✅ NOMINAL — champ isAvailableBetween
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle IsAvail Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_isAvailableBetween_noConflict_returnsTrue() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\" }) { results { id isAvailableBetween(from: \\"%s\\", to: \\"%s\\") } } }" }
                """.formatted(FUTURE_FROM, FUTURE_TO);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results[0].isAvailableBetween").value(true));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle IsAvail Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason) VALUES (9000000061, 9000000035, '2030-06-03', '2030-06-07', 'OWNER_BLOCKED')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void places_isAvailableBetween_withConflict_returnsFalse() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { ownerId: \\"99999999998\\" }) { results { id isAvailableBetween(from: \\"%s\\", to: \\"%s\\") } } }" }
                """.formatted(FUTURE_FROM, FUTURE_TO);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.places.results[0].isAvailableBetween").value(false));
    }

    // ==================================================
    // ✅ NOMINAL — availableSlots
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle Slots Test', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)",
            "INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason) VALUES (9000000061, 9000000035, '2030-06-10', '2030-06-15', 'OWNER_BLOCKED')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void availableSlots_withOneBlockedPeriod_returnsTwoSlots() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // Indisponibilité : 2030-06-10 → 2030-06-15
        // Créneaux libres attendus : [2030-06-01, 2030-06-09] et [2030-06-16, 2030-06-30]
        String query = """
                { "query": "{ availableSlots(placeId: \\"9000000035\\", month: \\"2030-06\\") { from to } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.availableSlots", hasSize(2)))
                .andExpect(jsonPath("$.data.availableSlots[0].from").value("2030-06-01"))
                .andExpect(jsonPath("$.data.availableSlots[0].to").value("2030-06-09"))
                .andExpect(jsonPath("$.data.availableSlots[1].from").value("2030-06-16"))
                .andExpect(jsonPath("$.data.availableSlots[1].to").value("2030-06-30"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.place_unavailability WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.bookings WHERE place_id IN (9000000035)",
            "DELETE FROM roomify.places WHERE id IN (9000000035)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, capacity, user_id) VALUES (9000000035, 'Salle Slots Libre', '9 rue test 75001 paris', '9 rue Test, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 10, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void availableSlots_noBlocked_returnsFullMonth() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // Aucune indisponibilité → tout le mois est libre
        String query = """
                { "query": "{ availableSlots(placeId: \\"9000000035\\", month: \\"2030-06\\") { from to } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.availableSlots", hasSize(1)))
                .andExpect(jsonPath("$.data.availableSlots[0].from").value("2030-06-01"))
                .andExpect(jsonPath("$.data.availableSlots[0].to").value("2030-06-30"));
    }

    @Test
    void availableSlots_unknownPlace_returnsNotFoundError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ availableSlots(placeId: \\"999999999999\\", month: \\"2030-06\\") { from to } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("NOT_FOUND"));
    }

    // ==================================================
    // ❌ ERREURS DE VALIDATION — pagination et filtres classiques
    // ==================================================

    @Test
    void places_invalidPageSize_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(pagination: { pageSize: 200 }) { results { id name } pageInfo { page } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void places_invalidCapacityRange_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { capacityMin: 50, capacityMax: 10 }) { results { id name } pageInfo { page } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void places_invalidPriceRange_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ places(filter: { pricePerHourMin: 100.0, pricePerHourMax: 20.0 }) { results { id name } pageInfo { page } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    // ==================================================
    // ❌ ERREURS DE VALIDATION — dates de disponibilité
    // ==================================================

    @Test
    void places_availabilityOnlyOneDate_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // availableFrom fourni sans availableTo
        String query = """
                { "query": "{ places(filter: { availableFrom: \\"%s\\" }) { results { id } pageInfo { page } } }" }
                """.formatted(FUTURE_FROM);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void places_availabilityInvalidDateRange_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // availableFrom > availableTo
        String query = """
                { "query": "{ places(filter: { availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id } pageInfo { page } } }" }
                """.formatted(FUTURE_TO, FUTURE_FROM);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void places_availabilityDateInPast_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // availableFrom dans le passé
        String pastDate = LocalDate.now().minusDays(5).toString();
        String query = """
                { "query": "{ places(filter: { availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id } pageInfo { page } } }" }
                """.formatted(pastDate, FUTURE_FROM);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void places_availabilityWindowExceeds30Days_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // Fenêtre > 30 jours
        String from31 = LocalDate.now().plusDays(1).toString();
        String to31   = LocalDate.now().plusDays(35).toString();
        String query = """
                { "query": "{ places(filter: { availableFrom: \\"%s\\", availableTo: \\"%s\\" }) { results { id } pageInfo { page } } }" }
                """.formatted(from31, to31);

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void places_availabilityInvalidDateFormat_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // Format de date invalide → erreur au niveau du scalar Date
        String query = """
                { "query": "{ places(filter: { availableFrom: \\"01/06/2030\\", availableTo: \\"10/06/2030\\" }) { results { id } pageInfo { page } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void availableSlots_invalidMonthFormat_returnsGraphQlError() throws Exception {
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ availableSlots(placeId: \\"9000000035\\", month: \\"juin-2030\\") { from to } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(userCustom))
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
    void places_unauthenticated_returnsGraphQlForbiddenError() throws Exception {
        String query = """
                { "query": "{ places { results { id name } pageInfo { page } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    void availableSlots_unauthenticated_returnsGraphQlForbiddenError() throws Exception {
        String query = """
                { "query": "{ availableSlots(placeId: \\"1\\", month: \\"2030-06\\") { from to } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }
}
