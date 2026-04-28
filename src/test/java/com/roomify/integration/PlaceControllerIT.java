package com.roomify.integration;

import java.util.HashMap;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(statements = {
        "DELETE FROM roomify.place_unavailability WHERE place_id IN (SELECT id FROM roomify.places WHERE user_id IN (99999999998, 99999999999))",
        "DELETE FROM roomify.bookings             WHERE place_id IN (SELECT id FROM roomify.places WHERE user_id IN (99999999998, 99999999999))",
        "DELETE FROM roomify.places               WHERE user_id IN (99999999998, 99999999999)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlaceControllerIT extends AbstractIntegrationTest {

    private static final String ENDPOINT = "/api/v1/places";
    private static final String VALID_NAME = "Salle de reunion moderne";
    private static final String VALID_ADDRESS = "15 avenue Victor Hugo, 75016 Paris";
    private static final String VALID_TYPE = "MEETING_ROOM";
    private static final double VALID_PRICE = 30.00;

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
    void createPlace_nominal_returns201() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of(
                "name", VALID_NAME,
                "type", VALID_TYPE,
                "address", VALID_ADDRESS,
                "pricePerHour", VALID_PRICE
        );

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.type").value(VALID_TYPE))
                .andExpect(jsonPath("$.address").value(VALID_ADDRESS))
                .andExpect(jsonPath("$.pricePerHour").value(VALID_PRICE))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createPlace_withAllOptionalFields_returns201() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("name", VALID_NAME);
        request.put("type", VALID_TYPE);
        request.put("address", VALID_ADDRESS);
        request.put("pricePerHour", VALID_PRICE);
        request.put("description", "Salle equipee avec projecteur et tableau blanc pour vos reunions d equipe");
        request.put("capacity", 10);

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Salle equipee avec projecteur et tableau blanc pour vos reunions d equipe"))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // =================================
    // ❌ ERREURS DE VALIDATION
    // =================================

    @Test
    void createPlace_missingPricePerHour_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of(
                "name", VALID_NAME,
                "type", VALID_TYPE,
                "address", VALID_ADDRESS
        );

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlace_negativePricePerHour_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("name", VALID_NAME);
        request.put("type", VALID_TYPE);
        request.put("address", VALID_ADDRESS);
        request.put("pricePerHour", -10.0);

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlace_nameTooShort_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("name", "AB");
        request.put("type", VALID_TYPE);
        request.put("address", VALID_ADDRESS);
        request.put("pricePerHour", VALID_PRICE);

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlace_missingType_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of(
                "name", VALID_NAME,
                "address", VALID_ADDRESS,
                "pricePerHour", VALID_PRICE
        );

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlace_missingAddress_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of(
                "name", VALID_NAME,
                "type", VALID_TYPE,
                "pricePerHour", VALID_PRICE
        );

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlace_descriptionTooShort_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("name", VALID_NAME);
        request.put("type", VALID_TYPE);
        request.put("address", VALID_ADDRESS);
        request.put("pricePerHour", VALID_PRICE);
        request.put("description", "Trop court");

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlace_capacityIncoherent_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("name", VALID_NAME);
        request.put("type", VALID_TYPE);
        request.put("address", VALID_ADDRESS);
        request.put("pricePerHour", VALID_PRICE);
        request.put("capacity", 0);

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================================
    // ❌ ERREURS MÉTIER
    // =================================

    @Test
    @Sql(statements = """
                INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id)
                VALUES (9000000001, 'Salle De Reunion Moderne', '15 avenue victor hugo 75016 paris',
                        '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998);
            """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createPlace_duplication_returns409() throws Exception {
        // GIVEN - une place avec le même nom/adresse existe déjà en base (insérée via @Sql)
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of(
                "name", VALID_NAME,
                "type", VALID_TYPE,
                "address", VALID_ADDRESS,
                "pricePerHour", VALID_PRICE
        );

        // WHEN + THEN
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // =================================
    // ⚠️ RATE LIMITING
    // =================================

    @Test
    void createPlace_rateLimiter_blocksTooManyRequests() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var json = new ObjectMapper().writeValueAsString(Map.of(
                "name", VALID_NAME,
                "type", VALID_TYPE,
                "address", VALID_ADDRESS,
                "pricePerHour", VALID_PRICE
        ));

        // Premier appel : OK
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Deuxième appel immédiat : bloqué par le rate limiter
        mockMvc.perform(post(ENDPOINT)
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }

    // =================================
    // ✅ NOMINAL PATCH
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_name_returns200() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("name", "Nouveau Nom Valide"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9000000010L))
                .andExpect(jsonPath("$.name").value("Nouveau Nom Valide"))
                .andExpect(jsonPath("$.address").value("15 avenue Victor Hugo, 75016 Paris"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_allFields_returns200() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("name", "Studio Creatif");
        request.put("type", "STUDIO");
        request.put("address", "5 rue de la Paix, 75002 Paris");
        request.put("description", "Un studio moderne avec tout l equipement necessaire pour vos projets creatifs");
        request.put("capacity", 8);
        request.put("pricePerHour", 45.00);

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Studio Creatif"))
                .andExpect(jsonPath("$.type").value("STUDIO"))
                .andExpect(jsonPath("$.address").value("5 rue de la Paix, 75002 Paris"))
                .andExpect(jsonPath("$.capacity").value(8))
                .andExpect(jsonPath("$.pricePerHour").value(45.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'APPROVED', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_approvedPlaceResetsToPending_returns200() throws Exception {
        // GIVEN — la place est APPROVED
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN — toute modification remet la place en PENDING
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("pricePerHour", 35.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.pricePerHour").value(35.00));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_adminCanPatchAnotherUsersPlace_returns200() throws Exception {
        // GIVEN — la place appartient à user 99999999998, appelant est admin 99999999999
        var adminUser = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("pricePerHour", 50.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricePerHour").value(50.00));
    }

    // =================================
    // ❌ ERREURS DE VALIDATION PATCH
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_nameTooShort_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("name", "AB"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_negativePricePerHour_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("pricePerHour", -5.0);

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_descriptionTooShort_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("description", "Trop court"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_capacityIncoherent_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );
        var request = new HashMap<String, Object>();
        request.put("capacity", 0);

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================================
    // ❌ ERREURS MÉTIER PATCH
    // =================================

    @Test
    void updatePlace_notFound_returns404() throws Exception {
        // GIVEN — aucune place avec cet id
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9999999999")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("pricePerHour", 20.00))))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000010",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_notOwner_returns403() throws Exception {
        // GIVEN — la place appartient à user 99999999999, l'appelant 99999999998 a le rôle OWNER
        //         (d'une autre place) mais n'est pas propriétaire de celle-ci
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("pricePerHour", 20.00))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id IN (9000000010, 9000000013)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000010, 'Salle Reunion Moderne', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'MEETING_ROOM', 'PENDING', 30.00, 99999999998)",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000013, 'Bureau Professionnel', '15 avenue victor hugo 75016 paris', '15 avenue Victor Hugo, 75016 Paris', 'COWORKING_SPACE', 'PENDING', 25.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updatePlace_duplication_returns409() throws Exception {

        // GIVEN — user 99999999998 a deux places sur la même adresse
        // Renommer la place A avec le nom de la place B doit déclencher un 409
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(patch(ENDPOINT + "/9000000010")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("name", "Bureau Professionnel"))))
                .andExpect(status().isConflict());
    }

    // =================================
    // ✅ NOMINAL DELETE
    // =================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000020",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000020, 'Salle A Supprimer', '10 rue de rivoli 75001 paris', '10 rue de Rivoli, 75001 Paris', 'MEETING_ROOM', 'PENDING', 20.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void deletePlace_owner_returns204() throws Exception {
        // GIVEN — la place appartient à l'appelant
        var owner = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(delete(ENDPOINT + "/9000000020")
                        .with(user(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000020",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000020, 'Salle A Supprimer', '10 rue de rivoli 75001 paris', '10 rue de Rivoli, 75001 Paris', 'MEETING_ROOM', 'APPROVED', 20.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void deletePlace_adminCanDeleteAnotherUsersPlace_returns204() throws Exception {
        // GIVEN — la place appartient à user 99999999998, l'appelant est admin 99999999999
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        // WHEN + THEN
        mockMvc.perform(delete(ENDPOINT + "/9000000020")
                        .with(user(admin)))
                .andExpect(status().isNoContent());
    }

    // =================================
    // ❌ ERREURS MÉTIER DELETE
    // =================================

    @Test
    void deletePlace_notFound_returns404() throws Exception {
        // GIVEN — aucune place avec cet id
        var owner = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN
        mockMvc.perform(delete(ENDPOINT + "/9999999999")
                        .with(user(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000020",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000020, 'Salle A Supprimer', '10 rue de rivoli 75001 paris', '10 rue de Rivoli, 75001 Paris', 'MEETING_ROOM', 'PENDING', 20.00, 99999999999)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void deletePlace_notOwner_returns403() throws Exception {
        // GIVEN — la place appartient à user 99999999999, l'appelant 99999999998 a le rôle OWNER
        //         (d'une autre place) mais n'est pas propriétaire de celle-ci
        var otherOwner = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );

        // WHEN + THEN — le contrôle d'ownership au niveau service doit rejeter la requête
        mockMvc.perform(delete(ENDPOINT + "/9000000020")
                        .with(user(otherOwner)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.places WHERE id = 9000000020",
            "INSERT INTO roomify.places (id, name, normalized_address, address, type, status, price_per_hour, user_id) VALUES (9000000020, 'Salle A Supprimer', '10 rue de rivoli 75001 paris', '10 rue de Rivoli, 75001 Paris', 'MEETING_ROOM', 'PENDING', 20.00, 99999999998)"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void deletePlace_userWithoutOwnerRole_returns403() throws Exception {
        // GIVEN — l'appelant est un simple USER sans rôle OWNER : bloqué par Spring Security
        var simpleUser = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // WHEN + THEN — @PreAuthorize rejette avant d'atteindre le service
        mockMvc.perform(delete(ENDPOINT + "/9000000020")
                        .with(user(simpleUser)))
                .andExpect(status().isForbidden());
    }

}
