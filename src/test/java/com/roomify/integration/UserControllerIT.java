package com.roomify.integration;

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
import com.roomify.infrastucture.repository.UserRepository;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(statements = {
        "DELETE FROM roomify.email_verification_tokens WHERE user_id IN (SELECT id FROM roomify.users WHERE email = 'test.user@gmail.com' AND id != 99999999998)",
        "DELETE FROM roomify.user_roles WHERE user_id IN (SELECT id FROM roomify.users WHERE email = 'test.user@gmail.com' AND id != 99999999998)",
        "DELETE FROM roomify.users WHERE email = 'test.user@gmail.com' AND id != 99999999998"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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

}
