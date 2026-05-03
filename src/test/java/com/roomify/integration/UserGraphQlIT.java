package com.roomify.integration;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
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

@SuppressWarnings("java:S5976")
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(statements = {
        "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201, 9000000202, 9000000203)",
        "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201, 9000000202, 9000000203)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserGraphQlIT extends AbstractIntegrationTest {

    private static final String GRAPHQL_ENDPOINT = "/graphql";

    @Autowired
    private MockMvc mockMvc;

    // ==================================================
    // ✅ NOMINAL — sans filtre
    // ==================================================

    @Test
    void users_noFilter_returnsAllUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users { results { id email firstName lastName roles enabled emailVerified } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.results", notNullValue()))
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements", notNullValue()));
    }

    // ==================================================
    // ✅ NOMINAL — filtre firstNameContains
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'alice@fn-test.local', 'Alice', 'Dupont', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'bob@fn-test.local', 'Bob', 'Martin', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterByFirstNameContains_returnsMatchingUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { firstNameContains: \\"ali\\", emailContains: \\"@fn-test.local\\" }) { results { id firstName } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.results[*].firstName").value(everyItem(is("Alice"))))
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1));
    }

    // ==================================================
    // ✅ NOMINAL — filtre lastNameContains
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'alice@ln-test.local', 'Alice', 'Dupont', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'bob@ln-test.local', 'Bob', 'Martin', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterByLastNameContains_returnsMatchingUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { lastNameContains: \\"upo\\", emailContains: \\"@ln-test.local\\" }) { results { id lastName } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.results[*].lastName").value(everyItem(is("Dupont"))))
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1));
    }

    // ==================================================
    // ✅ NOMINAL — filtre emailContains
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'alice@em-test.local', 'Alice', 'Dupont', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'bob@other-em-test.local', 'Bob', 'Martin', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterByEmailContains_returnsMatchingUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { emailContains: \\"@em-test.local\\" }) { results { id email } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.results[*].email").value(everyItem(is("alice@em-test.local"))))
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1));
    }

    // ==================================================
    // ✅ NOMINAL — filtre role
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'owner@role-test.local', 'Owner', 'Test', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'user@role-test.local', 'User', 'Test', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'OWNER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterByRole_returnsOnlyUsersWithThatRole() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { role: OWNER, emailContains: \\"@role-test.local\\" }) { results { id roles } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.users.results[0].roles").value(everyItem(is("OWNER"))));
    }

    // ==================================================
    // ✅ NOMINAL — filtre deleted
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified, deleted_at, deleted_by) VALUES (9000000200, 'deleted@del-test.local', 'Deleted', 'User', '{bcrypt}Test@1234', true, true, NOW(), 99999999999)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'active@del-test.local', 'Active', 'User', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterDeletedTrue_returnsOnlyDeletedUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { deleted: true, emailContains: \\"@del-test.local\\" }) { results { id email deletedAt } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.users.results[0].email").value("deleted@del-test.local"))
                .andExpect(jsonPath("$.data.users.results[0].deletedAt", notNullValue()));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified, deleted_at, deleted_by) VALUES (9000000200, 'deleted@del-test.local', 'Deleted', 'User', '{bcrypt}Test@1234', true, true, NOW(), 99999999999)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'active@del-test.local', 'Active', 'User', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterDeletedFalse_returnsOnlyActiveUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { deleted: false, emailContains: \\"@del-test.local\\" }) { results { id email deletedAt } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.users.results[0].email").value("active@del-test.local"))
                .andExpect(jsonPath("$.data.users.results[0].deletedAt").isEmpty());
    }

    // ==================================================
    // ✅ NOMINAL — filtre enabled
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'enabled@en-test.local', 'Enabled', 'User', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'disabled@en-test.local', 'Disabled', 'User', '{bcrypt}Test@1234', false, false)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterEnabledTrue_returnsOnlyEnabledUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { enabled: true, emailContains: \\"@en-test.local\\" }) { results { id email enabled } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.users.results[0].email").value("enabled@en-test.local"))
                .andExpect(jsonPath("$.data.users.results[0].enabled").value(true));
    }

    // ==================================================
    // ✅ NOMINAL — filtre emailVerified
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'verified@ev-test.local', 'Verified', 'User', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'unverified@ev-test.local', 'Unverified', 'User', '{bcrypt}Test@1234', true, false)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_filterEmailVerifiedFalse_returnsOnlyUnverifiedUsers() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { emailVerified: false, emailContains: \\"@ev-test.local\\" }) { results { id email emailVerified } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.users.results[0].email").value("unverified@ev-test.local"))
                .andExpect(jsonPath("$.data.users.results[0].emailVerified").value(false));
    }

    // ==================================================
    // ✅ NOMINAL — pagination
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200, 9000000201, 9000000202, 9000000203)",
            "DELETE FROM roomify.users WHERE id IN (9000000200, 9000000201, 9000000202, 9000000203)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000200, 'u1@pag-test.local', 'Page', 'One', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000201, 'u2@pag-test.local', 'Page', 'Two', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000202, 'u3@pag-test.local', 'Page', 'Three', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified) VALUES (9000000203, 'u4@pag-test.local', 'Page', 'Four', '{bcrypt}Test@1234', true, true)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000201, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000202, id FROM roomify.roles WHERE name = 'USER'",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000203, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_paginationPageSizeTwo_returnsCorrectPage() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { emailContains: \\"@pag-test.local\\" }, pagination: { page: 0, pageSize: 2 }) { results { id } pageInfo { page pageSize totalElements totalPages hasNext hasPrevious } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.results", hasSize(2)))
                .andExpect(jsonPath("$.data.users.pageInfo.page").value(0))
                .andExpect(jsonPath("$.data.users.pageInfo.pageSize").value(2))
                .andExpect(jsonPath("$.data.users.pageInfo.totalElements").value(4))
                .andExpect(jsonPath("$.data.users.pageInfo.totalPages").value(2))
                .andExpect(jsonPath("$.data.users.pageInfo.hasNext").value(true))
                .andExpect(jsonPath("$.data.users.pageInfo.hasPrevious").value(false));
    }

    // ==================================================
    // ✅ NOMINAL — champs de réponse complets
    // ==================================================

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id IN (9000000200)",
            "DELETE FROM roomify.users WHERE id IN (9000000200)",
            "INSERT INTO roomify.users (id, email, first_name, last_name, password, enabled, email_verified, deleted_at, deleted_by) VALUES (9000000200, 'full@full-test.local', 'Full', 'Fields', '{bcrypt}Test@1234', true, true, NOW(), 99999999999)",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 9000000200, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void users_fullResponse_allFieldsPresent() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(filter: { emailContains: \\"@full-test.local\\" }) { results { id email firstName lastName roles enabled emailVerified deletedAt deletedBy } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.users.results", hasSize(1)))
                .andExpect(jsonPath("$.data.users.results[0].id", notNullValue()))
                .andExpect(jsonPath("$.data.users.results[0].email").value("full@full-test.local"))
                .andExpect(jsonPath("$.data.users.results[0].firstName").value("Full"))
                .andExpect(jsonPath("$.data.users.results[0].lastName").value("Fields"))
                .andExpect(jsonPath("$.data.users.results[0].roles").value(everyItem(is("USER"))))
                .andExpect(jsonPath("$.data.users.results[0].enabled").value(true))
                .andExpect(jsonPath("$.data.users.results[0].emailVerified").value(true))
                .andExpect(jsonPath("$.data.users.results[0].deletedAt", notNullValue()))
                .andExpect(jsonPath("$.data.users.results[0].deletedBy").value("99999999999"));
    }

    // ==================================================
    // ❌ ERREURS DE VALIDATION
    // ==================================================

    @Test
    void users_invalidPageSizeZero_returnsBadRequestError() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(pagination: { page: 0, pageSize: 0 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void users_pageSizeExceedsMax_returnsBadRequestError() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(pagination: { page: 0, pageSize: 200 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("BAD_REQUEST"));
    }

    @Test
    void users_pageNegative_returnsBadRequestError() throws Exception {
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        String query = """
                { "query": "{ users(pagination: { page: -1, pageSize: 10 }) { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(admin))
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
    void users_unauthenticated_returnsForbiddenError() throws Exception {
        String query = """
                { "query": "{ users { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    void users_roleUser_returnsForbiddenError() throws Exception {
        var regularUser = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        String query = """
                { "query": "{ users { results { id } pageInfo { totalElements } } }" }
                """;

        mockMvc.perform(post(GRAPHQL_ENDPOINT)
                        .with(user(regularUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }
}
