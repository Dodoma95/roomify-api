package com.roomify.integration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.roomify.domain.spi.StorageSpi;
import com.roomify.domain.models.RoleEnum;
import com.roomify.infrastucture.models.user.Role;

import static com.roomify.integration.utils.UserUtils.createCustomUserDetails;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(statements = {
        "UPDATE roomify.users SET deleted_at = NULL, deleted_by = NULL, avatar_url = NULL WHERE id = 99999999998"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserAvatarControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageSpi storageSpi;

    // ✅ NOMINAL

    @Test
    void updateAvatar_withValidJpeg_returns200() throws Exception {
        // GIVEN
        Mockito.when(storageSpi.upload(any(), anyString(), anyString()))
                .thenReturn("https://pub-test.r2.dev/avatars/99999999998/test-uuid");

        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        byte[] jpegBytes = createSmallImage("jpeg");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        // WHEN + THEN
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/users/me/avatar")
                        .file(file)
                        .with(user(userCustom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://pub-test.r2.dev/avatars/99999999998/test-uuid"));
    }

    // ❌ ERREURS DE VALIDATION

    @Test
    void updateAvatar_withFileTooLarge_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        // 11 MB > 10 MB limit
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", largeContent);

        // WHEN + THEN
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/users/me/avatar")
                        .file(file)
                        .with(user(userCustom)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAvatar_withNonImageFile_returns400() throws Exception {
        // GIVEN
        var userCustom = createCustomUserDetails(
                99999999998L, "test.user@gmail.com", "Test", "User",
                "{bcrypt}Test@12345678941",
                Set.of(Role.builder().name(RoleEnum.USER).build())
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "not an image".getBytes()
        );

        // WHEN + THEN
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/users/me/avatar")
                        .file(file)
                        .with(user(userCustom)))
                .andExpect(status().isBadRequest());
    }

    private byte[] createSmallImage(String format) throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }
}
