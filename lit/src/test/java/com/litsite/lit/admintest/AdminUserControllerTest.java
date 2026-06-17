package com.litsite.lit.admintest;

import com.litsite.lit.controller.AdminUserController;
import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.EmailService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // ✅ Автоматический откат изменений после каждого теста
@DisplayName("Интеграционные тесты AdminUserController")
class AdminUserControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public AuthHelper authHelper() {
            return mock(AuthHelper.class);
        }

        @Bean @Primary
        public EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private AuthHelper authHelper;
    @Autowired private EmailService emailService;

    private MyUser adminUser;
    private MyUser regularUser;

    @BeforeEach
    void setUp() {
        // 🔧 Создаём администратора БЕЗ ролей — проверка isAdmin() идёт через мок
        adminUser = new MyUser();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@litsite.com");
        adminUser.setPasswordHash("hashed-admin");
        adminUser.setRegistrationDate(LocalDateTime.now());
        adminUser.setEnabled(true);
        // ❌ НЕ устанавливаем roles — это вызывает TransientObjectException
        adminUser = userRepository.save(adminUser);

        // 🔧 Создаём обычного пользователя БЕЗ ролей
        regularUser = new MyUser();
        regularUser.setUsername("regularuser");
        regularUser.setEmail("user@litsite.com");
        regularUser.setPasswordHash("hashed-user");
        regularUser.setRegistrationDate(LocalDateTime.now());
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);

        // 🔧 Базовые моки для AuthHelper
        lenient().when(authHelper.isAdmin()).thenReturn(true);
        lenient().when(authHelper.getCurrentUserOrThrow()).thenReturn(adminUser);
    }

    @AfterEach
    void tearDown() {
        reset(authHelper, emailService);
        // ✅ @Transactional автоматически откатит все изменения — deleteAll() не нужен
        // Если очень хочется очищать явно: userRepository.deleteAll(); (но не deleteAllInBatch!)
    }

    // ===== Вспомогательные методы =====

    private void mockAdmin() {
        when(authHelper.isAdmin()).thenReturn(true);
        when(authHelper.getCurrentUserOrThrow()).thenReturn(adminUser);
    }

    private void mockNonAdmin() {
        when(authHelper.isAdmin()).thenReturn(false);
        when(authHelper.getCurrentUserOrThrow()).thenReturn(regularUser);
    }

    // ===== Тесты PUT /admin/users/{userId}/toggle-enabled =====

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: админ включает пользователя → 200")
    void toggleUserEnabled_Admin_EnableUser_Success() throws Exception {
        mockAdmin();
        regularUser.setEnabled(false);
        regularUser = userRepository.save(regularUser);

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "true")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(regularUser.getUserId()))
                .andExpect(jsonPath("$.username").value("regularuser"))
                .andExpect(jsonPath("$.isEnabled").value(true));

        MyUser updated = userRepository.findById(regularUser.getUserId()).orElseThrow();
        assertTrue(updated.getIsEnabled());  // ✅ Проверьте: геттер называется getIsEnabled() или isEnabled()
        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: админ отключает пользователя → 200 + письмо")
    void toggleUserEnabled_Admin_DisableUser_Success() throws Exception {
        mockAdmin();
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(false));

        MyUser updated = userRepository.findById(regularUser.getUserId()).orElseThrow();
        assertFalse(updated.getIsEnabled());
        verify(emailService).sendAccountDisabledNotice(
                eq("user@litsite.com"), eq("regularuser"), eq("Нарушение правил платформы")
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: отключение с кастомной причиной")
    void toggleUserEnabled_Admin_DisableWithCustomReason() throws Exception {
        mockAdmin();
        String customReason = "Спам в комментариях";

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .param("reason", customReason)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(false));

        verify(emailService).sendAccountDisabledNotice(
                eq("user@litsite.com"), eq("regularuser"), eq(customReason)
        );
    }

    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: не-админ → 403 Forbidden")
    void toggleUserEnabled_NonAdmin_Forbidden() throws Exception {
        mockNonAdmin();

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
        MyUser unchanged = userRepository.findById(regularUser.getUserId()).orElseThrow();
        assertTrue(unchanged.getIsEnabled());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: анонимный пользователь → 403")
    void toggleUserEnabled_Anonymous_Forbidden() throws Exception {
        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authHelper, never()).isAdmin();
        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: пользователь не найден → 404")
    void toggleUserEnabled_UserNotFound() throws Exception {
        mockAdmin();
        Long nonExistentId = 99999L;

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", nonExistentId)
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: ошибка отправки письма → 200 (ошибка игнорируется)")
    void toggleUserEnabled_EmailServiceFails_StillSuccess() throws Exception {
        mockAdmin();
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendAccountDisabledNotice(any(), any(), any());

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(false));

        MyUser updated = userRepository.findById(regularUser.getUserId()).orElseThrow();
        assertFalse(updated.getIsEnabled());
        verify(emailService).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: включение уже включённого → 200")
    void toggleUserEnabled_EnableAlreadyEnabled() throws Exception {
        mockAdmin();
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "true")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(true));

        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: отключение уже отключённого → 200")
    void toggleUserEnabled_DisableAlreadyDisabled() throws Exception {
        mockAdmin();
        regularUser.setEnabled(false);
        regularUser = userRepository.save(regularUser);

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(false));

        verify(emailService).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: null причина → используется дефолтная")
    void toggleUserEnabled_NullReason_UsesDefault() throws Exception {
        mockAdmin();

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        // reason не передаём
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(emailService).sendAccountDisabledNotice(
                eq("user@litsite.com"), eq("regularuser"), eq("Нарушение правил платформы")
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: невалидный enabled → 400")
    void toggleUserEnabled_InvalidEnabledParam_BadRequest() throws Exception {
        mockAdmin();

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "maybe")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: missing enabled → 400")
    void toggleUserEnabled_MissingEnabledParam_BadRequest() throws Exception {
        mockAdmin();

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id}/toggle-enabled: пользователь без email → 200 (email не отправляется)")
    void toggleUserEnabled_DisableUserWithoutEmail_Success() throws Exception {
        mockAdmin();
        regularUser.setEmail(null);
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);

        mockMvc.perform(put("/admin/users/{userId}/toggle-enabled", regularUser.getUserId())
                        .param("enabled", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(false));

        verify(emailService, never()).sendAccountDisabledNotice(any(), any(), any());
        MyUser updated = userRepository.findById(regularUser.getUserId()).orElseThrow();
        assertFalse(updated.getIsEnabled());
    }
}