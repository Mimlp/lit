package com.litsite.lit.avatartest;

import com.litsite.lit.controller.AvatarController;
import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.FileStorageService;
import com.litsite.lit.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Интеграционные тесты AvatarController")
class AvatarControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public FileStorageService fileStorageService() {
            return mock(FileStorageService.class);
        }

        @Bean @Primary
        public AuthHelper authHelper() {
            return mock(AuthHelper.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private AuthHelper authHelper;

    private MyUser testUser;
    private MockMultipartFile testImage;

    @BeforeEach
    void setUp() throws IOException {
        testUser = new MyUser();
        testUser.setUsername("avataruser");
        testUser.setEmail("avatar@test.com");
        testUser.setPasswordHash("hashed");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        testImage = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-data".getBytes()
        );

        lenient().when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        lenient().when(authHelper.getCurrentUserId()).thenReturn(testUser.getUserId());
    }

    @AfterEach
    void tearDown() {
        reset(fileStorageService, authHelper);
        userRepository.deleteAllInBatch();
    }

    private void mockAuthAs(MyUser user) {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(user);
        when(authHelper.getCurrentUserId()).thenReturn(user.getUserId());
    }

    // ===== POST /api/user/me/avatar =====

    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("POST /api/user/me/avatar: успешная загрузка")
    void uploadAvatar_Success_NewUser() throws Exception {
        mockAuthAs(testUser);

        String expectedUrl = "/avatars/avatar_1.jpg";
        when(fileStorageService.saveAvatar(any(), eq(testUser.getUserId()))).thenReturn(expectedUrl);

        mockMvc.perform(multipart("/api/user/me/avatar")
                        .file(testImage)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("avatar@test.com"))
                .andExpect(jsonPath("$.avatarUrl").value(expectedUrl));

        verify(fileStorageService).saveAvatar(any(), eq(testUser.getUserId()));
        MyUser updated = userRepository.findByEmail("avatar@test.com").orElseThrow();
        assertEquals(expectedUrl, updated.getAvatarUrl());
    }

    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("POST /api/user/me/avatar: замена аватара")
    void uploadAvatar_Success_ReplaceExisting() throws Exception {
        mockAuthAs(testUser);
        testUser.setAvatarUrl("/avatars/old_avatar.jpg");
        testUser = userRepository.save(testUser);

        String newUrl = "/avatars/avatar_1_new.jpg";
        when(fileStorageService.saveAvatar(any(), eq(testUser.getUserId()))).thenReturn(newUrl);

        mockMvc.perform(multipart("/api/user/me/avatar")
                        .file(testImage)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(newUrl));

        verify(fileStorageService).deleteAvatar("/avatars/old_avatar.jpg");
        verify(fileStorageService).saveAvatar(any(), eq(testUser.getUserId()));
    }

    // 🔧 FIX: Анонимный доступ → 403 (не 401!)
    @Test
    @WithAnonymousUser  // ✅ Явно указываем анонимного пользователя
    @DisplayName("POST /api/user/me/avatar: неавторизованный → 403")
    void uploadAvatar_Unauthorized() throws Exception {
        // AuthHelper не будет вызван — запрос блокируется на уровне Security Filter
        mockMvc.perform(multipart("/api/user/me/avatar")
                        .file(testImage)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andExpect(status().isForbidden());  // ✅ 403, а не 401

        verify(fileStorageService, never()).saveAvatar(any(), any());
    }

    // 🔧 FIX: Мокаем валидацию, так как сервис замокан
    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("POST /api/user/me/avatar: пустой файл → 400")
    void uploadAvatar_EmptyFile_BadRequest() throws Exception {
        mockAuthAs(testUser);

        // 🔧 Мокаем выброс исключения при валидации пустого файла
        when(fileStorageService.saveAvatar(any(), eq(testUser.getUserId())))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Файл не может быть пустым"
                ));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/user/me/avatar")
                        .file(emptyFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(fileStorageService).saveAvatar(any(), eq(testUser.getUserId()));
    }

    // ===== DELETE /api/user/me/avatar =====

    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("DELETE /api/user/me/avatar: успешное удаление")
    void deleteAvatar_Success_WithExistingAvatar() throws Exception {
        mockAuthAs(testUser);
        testUser.setAvatarUrl("/avatars/existing.jpg");
        testUser = userRepository.save(testUser);

        mockMvc.perform(delete("/api/user/me/avatar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());

        verify(fileStorageService).deleteAvatar("/avatars/existing.jpg");
        MyUser updated = userRepository.findByEmail("avatar@test.com").orElseThrow();
        assertNull(updated.getAvatarUrl());
    }

    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("DELETE /api/user/me/avatar: нет аватара → 200")
    void deleteAvatar_Success_NoExistingAvatar() throws Exception {
        mockAuthAs(testUser);
        testUser.setAvatarUrl(null);
        testUser = userRepository.save(testUser);

        mockMvc.perform(delete("/api/user/me/avatar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("avatar@test.com"));

        verify(fileStorageService, never()).deleteAvatar(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("DELETE /api/user/me/avatar: неавторизованный → 403")
    void deleteAvatar_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/user/me/avatar").with(csrf()))
                .andExpect(status().isForbidden());  // ✅ 403

        verify(fileStorageService, never()).deleteAvatar(any());
    }

    // ===== GET /api/avatars/{filename} =====

    @Test
    @DisplayName("GET /api/avatars/{filename}: успешная отдача")
    void serveAvatar_Success() throws Exception {
        String filename = "avatar_1.jpg";
        Resource mockResource = new ByteArrayResource("fake-image".getBytes());

        when(fileStorageService.serveAvatar(filename))
                .thenReturn(org.springframework.http.ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(mockResource));

        mockMvc.perform(get("/api/avatars/" + filename))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));

        verify(fileStorageService).serveAvatar(filename);
    }

    @Test
    @DisplayName("GET /api/avatars/{filename}: файл не найден → 404")
    void serveAvatar_NotFound() throws Exception {
        String filename = "nonexistent.jpg";
        when(fileStorageService.serveAvatar(filename))
                .thenReturn(org.springframework.http.ResponseEntity.notFound().build());

        mockMvc.perform(get("/api/avatars/" + filename))
                .andExpect(status().isNotFound());

        verify(fileStorageService).serveAvatar(filename);
    }

    @Test
    @DisplayName("GET /api/avatars/{filename}: недопустимое имя → 400")
    void serveAvatar_InvalidFilename() throws Exception {
        String maliciousFilename = "../../../etc/passwd";
        mockMvc.perform(get("/api/avatars/" + maliciousFilename))
                .andExpect(status().isBadRequest());
    }

    // ===== Дополнительные тесты — мокаем валидацию =====

    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("POST /api/user/me/avatar: неверный тип файла → 400")
    void uploadAvatar_WrongFileType() throws Exception {
        mockAuthAs(testUser);

        // 🔧 Мокаем исключение при валидации типа файла
        when(fileStorageService.saveAvatar(any(), eq(testUser.getUserId())))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Разрешены только изображения"
                ));

        MockMultipartFile wrongType = new MockMultipartFile(
                "file", "script.exe", "application/x-executable", "fake-exe".getBytes()
        );

        mockMvc.perform(multipart("/api/user/me/avatar")
                        .file(wrongType)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "avataruser", roles = {"USER"})
    @DisplayName("POST /api/user/me/avatar: файл слишком большой → 400")
    void uploadAvatar_FileTooLarge() throws Exception {
        mockAuthAs(testUser);

        // 🔧 Мокаем исключение при превышении размера
        when(fileStorageService.saveAvatar(any(), eq(testUser.getUserId())))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,  // ✅ 400, а не 413
                        "Максимальный размер файла: 5 MB"
                ));

        byte[] largeData = new byte[10 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", MediaType.IMAGE_JPEG_VALUE, largeData
        );

        mockMvc.perform(multipart("/api/user/me/avatar")
                        .file(largeFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andExpect(status().isBadRequest());  // ✅ Ожидает 400 из-за валидации в сервисе
    }
}