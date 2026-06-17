package com.litsite.lit.authtest;

import com.litsite.lit.dto.*;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.security.jwt.JwtService;
import com.litsite.lit.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationControllerIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        EmailService emailService() {
            // ✅ ВАРИАНТ А: Простая заглушка без Mockito (рекомендуется)
            return new EmailService() {
                @Override
                public void sendVerificationEmail(String to, String subject, String text) {}
                @Override
                public void sendHtmlEmail(String to, String subject, String htmlContent) {}
                @Override
                public void sendContentModerationNotice(String to, String contentType, String contentTitle, String reason) {}
                @Override
                public void sendAccountDisabledNotice(String to, String username, String reason) {}
            };
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Интеграционный тест: полный поток регистрации и верификации")
    void signup_And_Verify_FullFlow() throws Exception {
        // Signup
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "newuser",
                        "email": "new@example.com",
                        "password": "password123"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        MyUser user = userRepository.findByEmail("new@example.com").orElseThrow();

        assertThat(user.getVerificationCode()).isNotNull().hasSize(6);

        // 🔧 Обновляем срок действия — без save(), только flush!
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
        userRepository.flush();

        String code = user.getVerificationCode();
        System.out.println(">>> Sending verification code: '" + code + "'");

        // Verify
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "new@example.com",
                        "verificationCode": "%s"
                    }
                    """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account verified successfully"));

        // Проверка, что пользователь активирован
        user = userRepository.findByEmail("new@example.com").orElseThrow();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getVerificationCode()).isNull();
    }

    @Test
    @DisplayName("Интеграционный тест: логин после верификации")
    void login_AfterVerification() throws Exception {
        MyUser user = new MyUser();
        user.setUsername("logintest");
        user.setEmail("login@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true);
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "login@example.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("Интеграционный тест: логин с неверным паролем")
    void login_InvalidPassword() throws Exception {
        MyUser user = new MyUser();
        user.setUsername("test");
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("correctPassword"));
        user.setEnabled(true);
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "test@example.com",
                            "password": "wrongPassword"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Интеграционный тест: обновление токена")
    void refresh_Token() throws Exception {
        MyUser user = new MyUser();
        user.setEmail("refresh@example.com");
        user.setUsername("refreshtest");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true);
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);

        com.litsite.lit.dto.JwtAuthDto tokens = jwtService.generateAuthToken("refresh@example.com");
        String refreshToken = tokens.getRefreshToken();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "refreshToken": "%s"
                        }
                        """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }
// ===== Тесты для /auth/login =====

// ===== Тесты для /auth/verify =====

    @Test
    @DisplayName("Верификация: неверный код")
    void verify_InvalidCode() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "verifytest",
                        "email": "verify@example.com",
                        "password": "password123"
                    }
                    """))
                .andExpect(status().isOk());

        // Пытаемся верифицировать с неверным кодом
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "verify@example.com",
                        "verificationCode": "000000"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid verification code"));
    }

    @Test
    @DisplayName("Верификация: истёкший код")
    void verify_ExpiredCode() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "expiredtest",
                        "email": "expired@example.com",
                        "password": "password123"
                    }
                    """))
                .andExpect(status().isOk());

        // Находим пользователя и искусственно "протухаем" его код
        MyUser user = userRepository.findByEmail("expired@example.com").orElseThrow();
        user.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);
        userRepository.flush();

        // Пытаемся верифицировать
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "expired@example.com",
                        "verificationCode": "%s"
                    }
                    """.formatted(user.getVerificationCode())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Verification code has expired"));
    }

    @Test
    @DisplayName("Верификация: несуществующий пользователь")
    void verify_UserNotFound() throws Exception {
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "nonexistent@example.com",
                        "verificationCode": "123456"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("User not found"));
    }

// ===== Тесты для /auth/resend =====

    @Test
    @DisplayName("Resend: несуществующий email")
    void resend_UserNotFound() throws Exception {
        mockMvc.perform(post("/auth/resend")
                        .param("email", "nonexistent@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found"));
    }

    @Test
    @DisplayName("Resend: уже верифицированный аккаунт")
    void resend_AlreadyVerified() throws Exception {
        // Создаём и сразу верифицируем пользователя
        MyUser user = new MyUser();
        user.setUsername("alreadyverified");
        user.setEmail("verified@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true); // Уже верифицирован
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/resend")
                        .param("email", "verified@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account is already verified"));
    }

    @Test
    @DisplayName("Resend: успешная отправка нового кода")
    void resend_Success() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "username": "resendtest",
                        "email": "resend@example.com",
                        "password": "password123"
                    }
                    """))
                .andExpect(status().isOk());

        MyUser user = userRepository.findByEmail("resend@example.com").orElseThrow();
        String oldCode = user.getVerificationCode();

        // Запрашиваем повторную отправку
        mockMvc.perform(post("/auth/resend")
                        .param("email", "resend@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent"));

        // Проверяем, что код обновился
        user = userRepository.findByEmail("resend@example.com").orElseThrow();
        assertNotEquals(oldCode, user.getVerificationCode());
        assertThat(user.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());
    }

// ===== Тесты для /auth/refresh =====

    @Test
    @DisplayName("Логин: проверка наличия токенов в ответе")
    void login_TokensPresent() throws Exception {
        // Создаём верифицированного пользователя
        MyUser user = new MyUser();
        user.setUsername("tokentest");
        user.setEmail("token@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true);
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "token@example.com",
                        "password": "password123"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
    }
    @Test
    @DisplayName("Верификация с истёкшим кодом → 400")
    void verify_ExpiredCode_BadRequest() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "username": "expired",
                "email": "expired@example.com",
                "password": "password123"
            }
            """))
                .andExpect(status().isOk());

        // Находим пользователя и искусственно "просрочиваем" код
        MyUser user = userRepository.findByEmail("expired@example.com").orElseThrow();
        user.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1)); // ← Истёк!
        userRepository.save(user);
        userRepository.flush();

        // Пытаемся верифицировать
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "email": "expired@example.com",
                "verificationCode": "%s"
            }
            """.formatted(user.getVerificationCode())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Verification code has expired"));
    }
    @Test
    @DisplayName("Resend: успешная повторная отправка кода")
    void resendVerificationCode_Success() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "username": "resendtest",
                "email": "resend@example.com",
                "password": "password123"
            }
            """))
                .andExpect(status().isOk());

        // Запоминаем старый код
        MyUser user = userRepository.findByEmail("resend@example.com").orElseThrow();
        String oldCode = user.getVerificationCode();

        // Ждём немного, чтобы код точно обновился
        Thread.sleep(10);

        // Запрашиваем повторную отправку
        mockMvc.perform(post("/auth/resend")
                        .param("email", "resend@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent"));

        // Проверяем, что код изменился
        user = userRepository.findByEmail("resend@example.com").orElseThrow();
        assertThat(user.getVerificationCode()).isNotEqualTo(oldCode);
        assertThat(user.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());
    }
    @Test
    @DisplayName("Resend для верифицированного аккаунта → 400")
    void resendVerificationCode_AlreadyVerified_BadRequest() throws Exception {
        // Создаём и сразу верифицируем пользователя
        MyUser user = new MyUser();
        user.setUsername("verified");
        user.setEmail("verified@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true); // ← Уже верифицирован!
        user.setRegistrationDate(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/resend")
                        .param("email", "verified@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account is already verified"));
    }
    @Test
    @DisplayName("Верификация с неверным кодом → 400")
    void verify_WrongCode_BadRequest() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "username": "wrongcode",
                "email": "wrong@example.com",
                "password": "password123"
            }
            """))
                .andExpect(status().isOk());

        // Пытаемся верифицировать с неправильным кодом
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
                "email": "wrong@example.com",
                "verificationCode": "000000"
            }
            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid verification code"));
    }
}