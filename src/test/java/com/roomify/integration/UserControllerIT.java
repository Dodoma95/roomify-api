package com.roomify.integration;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;

import static com.roomify.integration.utils.CustomUserUtils.createCustomUserDetails;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void me_casNominal() throws Exception {
        var userCustom = createCustomUserDetails(
                "super.admin@gmail.com",
                "password",
                Set.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build())
        );

        mockMvc.perform(get("/api/v1/users/me").with(user(userCustom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("super.admin@gmail.com"))
                .andExpect(jsonPath("$.roles", hasItem(RoleEnum.SUPER_ADMIN.asAuthority())));
    }

    @Test
    void me_internalServerError_returns500() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isInternalServerError());
    }

}
