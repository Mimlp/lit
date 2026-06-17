package com.litsite.lit.authtest;

import com.litsite.lit.dto.*;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Role;
import com.litsite.lit.repository.RoleRepository;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.security.jwt.JwtService;
import com.litsite.lit.service.AuthenticationService;
import com.litsite.lit.service.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private JwtService jwtService;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private MyUser testUser;
    private RegisterUserDto registerDto;
    private LoginUserDto loginDto;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setEnabled(false);
        // ✅ ИСПРАВЛЕНО: Set<Role> вместо String
        testUser.setRoles(Set.of(new Role("ROLE_USER")));
        testUser.setVerificationCode("123456");
        testUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        registerDto = new RegisterUserDto();
        registerDto.setUsername("testuser");
        registerDto.setEmail("test@example.com");
        registerDto.setPassword("password123");

        loginDto = new LoginUserDto();
        loginDto.setEmail("test@example.com");
        loginDto.setPassword("password123");
    }

    @Test
    @DisplayName("Signup: успешная регистрация пользователя")
    void signup_Success() throws MessagingException {
        Role mockRole = new Role("ROLE_USER");
        mockRole.setId(1L); // если нужно

        // 🔧 Настраиваем поведение RoleRepository
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(mockRole));

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(MyUser.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        MyUser result = authenticationService.signup(registerDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getVerificationCode()).hasSize(6);
        assertThat(result.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());

        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString(), anyString());
        verify(userRepository).save(any(MyUser.class));
        verify(roleRepository).findByName("ROLE_USER");
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString(), anyString());
        verify(userRepository).save(any(MyUser.class));
    }

    @Test
    @DisplayName("Authenticate: успешная аутентификация")
    void authenticate_Success() {
        testUser.setEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "password123"));

        MyUser result = authenticationService.authenticate(loginDto);

        assertThat(result).isEqualTo(testUser);
        // ✅ ИСПРАВЛЕНО: используем any() вместо new UsernamePasswordAuthenticationToken(...)
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Authenticate: пользователь не найден")
    void authenticate_UserNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(loginDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Authenticate: аккаунт не верифицирован")
    void authenticate_AccountNotVerified() {
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.authenticate(loginDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not verified. Please verify your account.");
    }

    @Test
    @DisplayName("Authenticate: неверный пароль")
    void authenticate_InvalidPassword() {
        testUser.setEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        willThrow(new BadCredentialsException("Bad credentials"))
                .given(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authenticationService.authenticate(loginDto))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("VerifyUser: успешная верификация")
    void verifyUser_Success() {
        VerifyUserDto verifyDto = new VerifyUserDto("test@example.com", "123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(MyUser.class))).thenAnswer(i -> i.getArgument(0));

        authenticationService.verifyUser(verifyDto);

        assertThat(testUser.isEnabled()).isTrue();
        assertThat(testUser.getVerificationCode()).isNull();
        assertThat(testUser.getVerificationCodeExpiresAt()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("VerifyUser: код истёк")
    void verifyUser_CodeExpired() {
        LocalDateTime fixedNow = LocalDateTime.of(2026, 5, 29, 12, 0, 0);
        testUser.setVerificationCodeExpiresAt(fixedNow.minusMinutes(1));
        VerifyUserDto verifyDto = new VerifyUserDto("test@example.com", "123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.verifyUser(verifyDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Verification code has expired");
    }

    @Test
    @DisplayName("VerifyUser: неверный код")
    void verifyUser_InvalidCode() {
        VerifyUserDto verifyDto = new VerifyUserDto("test@example.com", "999999");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.verifyUser(verifyDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid verification code");
    }

    @Test
    @DisplayName("ResendVerificationCode: успешная отправка")
    void resendVerificationCode_Success() throws MessagingException {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(MyUser.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        authenticationService.resendVerificationCode("test@example.com");

        assertThat(testUser.getVerificationCode()).isNotNull().hasSize(6);
        assertThat(testUser.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("ResendVerificationCode: аккаунт уже верифицирован")
    void resendVerificationCode_AlreadyVerified() {
        testUser.setEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.resendVerificationCode("test@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account is already verified");
    }
}