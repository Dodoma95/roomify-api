package com.roomify.integration;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.repository.UserRepository;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(statements = {
        "DELETE FROM roomify.email_verification_tokens WHERE user_id IN (SELECT id FROM roomify.users WHERE email = 'test.user@gmail.com' AND id != 99999999998)",
        "DELETE FROM roomify.user_roles WHERE user_id IN (SELECT id FROM roomify.users WHERE email = 'test.user@gmail.com' AND id != 99999999998)",
        "DELETE FROM roomify.users WHERE email = 'test.user@gmail.com' AND id != 99999999998",
        "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL, email = 'test.user@gmail.com' WHERE id = 99999999998",
        "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
        "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    void get_me_casNominal() throws Exception {
        var userCustom = createCustomUserDetails(
                1L,
                "super.admin@gmail.com",
                "Get",
                "Me",
                "password",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );

        mockMvc.perform(get("/api/v1/users/me").with(user(userCustom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("super.admin@gmail.com"))
                .andExpect(jsonPath("$.roles", hasItem(RoleEnum.SUPER_ADMIN.asAuthority())));
    }

    @Test
    void get_me_internalServerError_returns500() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @Sql(statements = """
                UPDATE roomify.users
                SET deleted_at = NULL,
                    deleted_by = NULL
                WHERE id = 99999999998;
            """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void delete_me_casNominal() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // WHEN
        mockMvc.perform(delete("/api/v1/users/me").with(user(userCustom)))
                .andExpect(status().isNoContent());

        // THEN
        Long userId = userCustom.user().getId();
        var userInDb = userRepository.findById(userId).orElseThrow();

        assertThat(userInDb.getDeletedAt()).isNotNull();
        assertThat(userInDb.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void delete_me_userNotFound() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                10L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );

        // WHEN
        mockMvc.perform(delete("/api/v1/users/me").with(user(userCustom)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = """
                UPDATE roomify.users
                SET deleted_at = NULL,
                    deleted_by = NULL
                WHERE id = 99999999998;
            """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void delete_user_by_id_casNominal() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999999L,
                "test.admin@gmail.com",
                "Test",
                "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        // WHEN
        mockMvc.perform(delete("/api/v1/users/99999999998")
                        .with(user(userCustom)))
                .andExpect(status().isNoContent());

        // THEN
        Long userId = 99999999998L;
        var userInDb = userRepository.findById(userId).orElseThrow();

        assertThat(userInDb.getDeletedAt()).isNotNull();
        assertThat(userInDb.getDeletedBy()).isEqualTo(userCustom.user().getId());
    }

    @Test
    @Sql(statements = """
                UPDATE roomify.users
                SET deleted_at = NULL,
                    deleted_by = NULL
                WHERE id = 99999999998;
            """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void delete_user_by_id_cannot_delete_yourself() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999999L,
                "test.admin@gmail.com",
                "Test",
                "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        // WHEN
        mockMvc.perform(delete("/api/v1/users/99999999999")
                        .with(user(userCustom)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_user_by_id_cannot_delete_super_admin() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999999L,
                "test.admin@gmail.com",
                "Test",
                "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        // WHEN
        mockMvc.perform(delete("/api/v1/users/1")
                        .with(user(userCustom)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_user_by_id_cannot_delete_user_not_found_then_404() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999999L,
                "test.admin@gmail.com",
                "Test",
                "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );

        // WHEN
        mockMvc.perform(delete("/api/v1/users/5151515")
                        .with(user(userCustom)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(statements = """
                UPDATE roomify.users
                SET first_name = 'Test',
                    last_name = 'User',
                    email = 'test.user@gmail.com',
                    email_verified = false,
                    deleted_at = NULL,
                    deleted_by = NULL
                WHERE id = 99999999998;
            """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patch_me_casNominal_updates_names() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of(
                "firstName", "Johnny",
                "lastName", "Walker"
        );

        // WHEN
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test.user@gmail.com"))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Walker"))
                .andExpect(jsonPath("$.roles", hasItem(RoleEnum.USER.asAuthority())));

        // THEN
        var userInDb = userRepository.findById(99999999998L).orElseThrow();
        assertThat(userInDb.getFirstName()).isEqualTo("Johnny");
        assertThat(userInDb.getLastName()).isEqualTo("Walker");
        assertThat(userInDb.getEmail()).isEqualTo("test.user@gmail.com");
        assertThat(userInDb.isEmailVerified()).isFalse();
    }

    @Test
    @Sql(statements = """
                UPDATE roomify.users
                SET first_name = 'Test',
                    last_name = 'User',
                    email = 'test.user@gmail.com',
                    email_verified = true,
                    deleted_at = NULL,
                    deleted_by = NULL
                WHERE id = 99999999998;
            """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = "UPDATE roomify.users SET email = 'test.user@gmail.com' WHERE id = 99999999998",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void patch_me_emailChanged_resets_email_verified() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of("email", "updated.user@gmail.com");

        // WHEN
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated.user@gmail.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"));

        // THEN
        var userInDb = userRepository.findById(99999999998L).orElseThrow();
        assertThat(userInDb.getEmail()).isEqualTo("updated.user@gmail.com");
        assertThat(userInDb.isEmailVerified()).isFalse();
    }

    @Test
    void patch_me_validationError_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of("email", "not-an-email");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_me_userNotFound_returns404() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                10L,
                "unknown.user@gmail.com",
                "Unknown",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of("firstName", "John");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ✅ NOMINAL — updateUserRole

    @Test
    @Sql(statements = {
            "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL WHERE id = 99999999998",
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_superAdmin_addsOwnerToUser_returns200() throws Exception {
        // GIVEN
        var superAdmin = createCustomUserDetails(
                99999999996L, "test.super.admin@gmail.com", "Test", "SuperAdmin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );
        var request = Map.of("role", "OWNER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99999999998L))
                .andExpect(jsonPath("$.roles", hasItem("OWNER")))
                .andExpect(jsonPath("$.roles", hasItem("USER")));
    }

    @Test
    @Sql(statements = {
            "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL WHERE id = 99999999998",
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_superAdmin_removesUserRole_returns200() throws Exception {
        // GIVEN
        var superAdmin = createCustomUserDetails(
                99999999996L, "test.super.admin@gmail.com", "Test", "SuperAdmin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );
        var request = Map.of("role", "USER", "action", "REMOVE");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99999999998L))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", not(hasItem("USER"))));
    }

    @Test
    @Sql(statements = {
            "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL WHERE id = 99999999998",
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_admin_addsOwnerToUser_returns200() throws Exception {
        // GIVEN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "OWNER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("OWNER")));
    }

    @Test
    @Sql(statements = {
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999997",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999997, id FROM roomify.roles WHERE name = 'OWNER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_admin_removesOwnerFromOwner_returns200() throws Exception {
        // GIVEN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "OWNER", "action", "REMOVE");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999997/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99999999997L));
    }

    // ❌ ERREURS DE VALIDATION — updateUserRole

    @Test
    void patchUserRole_missingRoleField_returns400() throws Exception {
        // GIVEN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchUserRole_missingActionField_returns400() throws Exception {
        // GIVEN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "USER");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchUserRole_invalidRoleValue_returns400() throws Exception {
        // GIVEN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "UNKNOWN_ROLE", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ❌ ERREURS MÉTIER — updateUserRole

    @Test
    @Sql(statements = {
            "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL WHERE id = 99999999998",
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_addRoleAlreadyAssigned_returns409() throws Exception {
        // GIVEN — user already has USER role
        var superAdmin = createCustomUserDetails(
                99999999996L, "test.super.admin@gmail.com", "Test", "SuperAdmin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );
        var request = Map.of("role", "USER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Sql(statements = {
            "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL WHERE id = 99999999998",
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_removeRoleNotAssigned_returns400() throws Exception {
        // GIVEN — user does not have OWNER role
        var superAdmin = createCustomUserDetails(
                99999999996L, "test.super.admin@gmail.com", "Test", "SuperAdmin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );
        var request = Map.of("role", "OWNER", "action", "REMOVE");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchUserRole_userNotFound_returns404() throws Exception {
        // GIVEN
        var superAdmin = createCustomUserDetails(
                99999999996L, "test.super.admin@gmail.com", "Test", "SuperAdmin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );
        var request = Map.of("role", "OWNER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/5151515/role")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchUserRole_admin_targetsAdmin_returns403() throws Exception {
        // GIVEN — cible 99999999999 est ADMIN
        var admin = createCustomUserDetails(
                1L, "another.admin@gmail.com", "Another", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "USER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999999/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUserRole_admin_targetsSuperAdmin_returns403() throws Exception {
        // GIVEN — cible 99999999996 est SUPER_ADMIN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "USER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999996/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(statements = {
            "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL WHERE id = 99999999998",
            "DELETE FROM roomify.user_roles WHERE user_id = 99999999998",
            "INSERT INTO roomify.user_roles (user_id, role_id) SELECT 99999999998, id FROM roomify.roles WHERE name = 'USER'"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patchUserRole_admin_assignsAdminRole_returns403() throws Exception {
        // GIVEN — ADMIN tente d'attribuer le rôle ADMIN
        var admin = createCustomUserDetails(
                99999999999L, "test.admin@gmail.com", "Test", "Admin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.ADMIN).build())
        );
        var request = Map.of("role", "ADMIN", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUserRole_superAdmin_removesOwnSuperAdminRole_returns403() throws Exception {
        // GIVEN — SUPER_ADMIN tente de retirer son propre rôle SUPER_ADMIN
        var superAdmin = createCustomUserDetails(
                99999999996L, "test.super.admin@gmail.com", "Test", "SuperAdmin",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );
        var request = Map.of("role", "SUPER_ADMIN", "action", "REMOVE");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999996/role")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUserRole_userRole_forbidden_returns403() throws Exception {
        // GIVEN
        var userWithUserRole = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of("role", "OWNER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(userWithUserRole))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUserRole_ownerRole_forbidden_returns403() throws Exception {
        // GIVEN
        var owner = createCustomUserDetails(
                99999999997L, "test.owner@gmail.com", "Test", "Owner",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.OWNER).build())
        );
        var request = Map.of("role", "USER", "action", "ADD");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/99999999998/role")
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ✅ NOMINAL — description

    @Test
    @Sql(statements = """
            UPDATE roomify.users
            SET first_name = 'Test',
                last_name  = 'User',
                email      = 'test.user@gmail.com',
                description = NULL,
                deleted_at  = NULL,
                deleted_by  = NULL
            WHERE id = 99999999998;
        """, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void patch_me_withDescription_returns200() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        var request = Map.of("description", "Passionné de voyage et de randonnée.");

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Passionné de voyage et de randonnée."));

        // THEN
        var userInDb = userRepository.findById(99999999998L).orElseThrow();
        assertThat(userInDb.getDescription()).isEqualTo("Passionné de voyage et de randonnée.");
    }

    // ❌ ERREURS DE VALIDATION — description

    @Test
    void patch_me_descriptionTooLong_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L,
                "test.user@gmail.com",
                "Test",
                "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        // 1001 caractères
        var request = Map.of("description", "a".repeat(1001));

        // WHEN + THEN
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(userCustom))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
