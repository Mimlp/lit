package com.litsite.lit.authtest;

import com.litsite.lit.dto.JwtAuthDto;
import com.litsite.lit.security.jwt.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private final String testEmail = "test@example.com";
    private final String secretKey = Base64.getEncoder()
            .encodeToString("thisIsASecretKeyForTestingPurposesOnly1234567890".getBytes());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
    }

    @Test
    @DisplayName("generateAuthToken: генерация access и refresh токенов")
    void generateAuthToken_Success() {
        JwtAuthDto result = jwtService.generateAuthToken(testEmail);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isNotNull().isNotEmpty();
        assertThat(result.getRefreshToken()).isNotNull().isNotEmpty();
        assertThat(result.getToken()).isNotEqualTo(result.getRefreshToken());
    }

    @Test
    @DisplayName("getEmailFromToken: извлечение email из токена")
    void getEmailFromToken_Success() {
        String token = jwtService.generateAuthToken(testEmail).getToken();

        String email = jwtService.getEmailFromToken(token);

        assertThat(email).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("validateJwtToken: валидный токен")
    void validateJwtToken_ValidToken() {
        String token = jwtService.generateAuthToken(testEmail).getToken();

        boolean isValid = jwtService.validateJwtToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateJwtToken: невалидный токен")
    void validateJwtToken_InvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtService.validateJwtToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateJwtToken: токен с неправильной подписью")
    void validateJwtToken_WrongSignature() {
        String otherKey = Base64.getEncoder()
                .encodeToString("differentSecretKeyForTestingPurposesOnly12345".getBytes());
        JwtService otherJwtService = new JwtService();
        ReflectionTestUtils.setField(otherJwtService, "secretKey", otherKey);
        String token = otherJwtService.generateAuthToken(testEmail).getToken();

        boolean isValid = jwtService.validateJwtToken(token);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("refreshBaseToken: обновление access токена")
    void refreshBaseToken_Success() {
        String refreshToken = jwtService.generateAuthToken(testEmail).getRefreshToken();

        JwtAuthDto result = jwtService.refreshBaseToken(testEmail, refreshToken);

        assertThat(result.getToken()).isNotNull().isNotEmpty();
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(result.getToken()).isNotEqualTo(refreshToken);
    }
}