package com.roomify.integration;

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

@Sql(
        statements = "DELETE FROM roomify.places WHERE user_id IN (99999999998, 99999999999)",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class PlaceGraphQlIT extends AbstractIntegrationTest {

    private static final String GRAPHQL_ENDPOINT = "/graphql";

    @Autowired
    private MockMvc mockMvc;

    // ==================================================
    // ✅ NOMINAL
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

        // Parmi les 3 fixtures : 1 MEETING_ROOM + les 2 MEETING_ROOM de migration = 3 total
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

        // PENDING : 1 fixture + quelques migrations (Large Event Hall, Startup Meeting Hub) = au moins 1
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

        // pricePerHourMin=20, pricePerHourMax=60 → seule la Salle Moyenne (40€) pour cet owner
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

        // "Salle" doit matcher "Salle de Brainstorming" et "Grande Salle Evenement" pour cet owner
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

        // 5 fixtures STUDIO pour user 99999999999, on demande pageSize=2
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
    // ❌ ERREURS DE VALIDATION
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
                .andExpect(status().isOk()) // GraphQL retourne toujours 200
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
    // 🔒 SÉCURITÉ
    // ==================================================

    @Test
    void places_unauthenticated_returnsGraphQlForbiddenError() throws Exception {
        // Sans utilisateur injecté → @PreAuthorize refuse l'accès.
        // Spring GraphQL intercepte AccessDeniedException et retourne HTTP 200
        // avec une erreur FORBIDDEN dans le corps (sémantique GraphQL standard).
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

}
