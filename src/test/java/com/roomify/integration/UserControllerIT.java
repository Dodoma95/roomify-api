package com.roomify.integration;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.repository.UserRepository;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

}
