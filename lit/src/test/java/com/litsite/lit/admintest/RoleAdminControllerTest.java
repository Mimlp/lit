package com.litsite.lit.admintest;

import com.litsite.lit.controller.RoleAdminController;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Role;
import com.litsite.lit.repository.RoleRepository;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.EmailService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты RoleAdminController")
class RoleAdminControllerTest {

    // 🔧 ТЕСТОВАЯ КОНФИГУРАЦИЯ: мокаем зависимости
    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean @Primary
        public com.litsite.lit.controller.AuthHelper authHelper() {
            return mock(com.litsite.lit.controller.AuthHelper.class);
        }
    }

    @Autowired private RoleAdminController roleAdminController;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EmailService emailService;  // Это МОК
    @Autowired private UserMapper userMapper;
    @Autowired private com.litsite.lit.controller.AuthHelper authHelper;  // Это МОК

    private MyUser adminUser;
    private MyUser regularUser;
    private MyUser targetUser;
    private Role adminRole;
    private Role moderatorRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // 🔧 Очищаем роли перед созданием, чтобы избежать конфликтов уникальности
        roleRepository.deleteAllInBatch();

        // 🔧 Создаём роли (теперь безопасно)
        adminRole = createRole("ROLE_ADMIN");
        moderatorRole = createRole("ROLE_MODERATOR");
        userRole = createRole("ROLE_USER");

        // 🔧 Создаём пользователей
        adminUser = createUser("admin", "admin@test.com", Set.of(adminRole));
        regularUser = createUser("regular", "regular@test.com", Set.of(userRole));
        targetUser = createUser("target", "target@test.com", Set.of(userRole));

        // 🔧 Базовые моки для EmailService
        lenient().doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

        // 🔧 Базовые моки для AuthHelper (по умолчанию — не админ)
        lenient().when(authHelper.isAdmin()).thenReturn(false);
        lenient().when(authHelper.isModerator()).thenReturn(false);
        lenient().when(authHelper.isAdminOrModerator()).thenReturn(false);
        lenient().when(authHelper.hasRole(anyString())).thenReturn(false);
        lenient().when(authHelper.hasAnyRole(anyString(), anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        reset(emailService, authHelper);
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
    }

    // ===== Вспомогательные методы =====

    private Role createRole(String name) {
        // 🔧 Проверяем, не существует ли уже роль
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    return roleRepository.save(role);
                });
    }

    private MyUser createUser(String username, String email, Set<Role> roles) {
        MyUser user = new MyUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed");
        user.setRegistrationDate(LocalDateTime.now());
        user.setEnabled(true);
        user.setRoles(new HashSet<>(roles));  // 🔧 Mutable!
        return userRepository.save(user);
    }

    // ===== Вспомогательный метод для аутентификации =====

    private void authenticateAsAdmin() {
        when(authHelper.isAdmin()).thenReturn(true);
        when(authHelper.hasRole("ROLE_ADMIN")).thenReturn(true);
        when(authHelper.hasAnyRole(anyString(), anyString())).thenReturn(true);
    }

    private void authenticateAsRegular() {
        when(authHelper.isAdmin()).thenReturn(false);
        when(authHelper.hasRole("ROLE_ADMIN")).thenReturn(false);
    }

    // ===== Тесты GET /admin/roles/available =====

    @Test
    @DisplayName("GET /admin/roles/available: админ получает список ролей")
    void getAvailableRoles_Admin_Success() {
        authenticateAsAdmin();

        var response = roleAdminController.getAvailableRoles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<String> roles = response.getBody();
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_MODERATOR"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("GET /admin/roles/available: не-админ получает 403")
    void getAvailableRoles_NonAdmin_Forbidden() {
        authenticateAsRegular();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.getAvailableRoles()
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ===== Тесты PUT /admin/users/{userId}/role =====

    @Test
    @Transactional
    @DisplayName("PUT /admin/users/{userId}/role: админ назначает роль модератора")
    void assignRole_Admin_ToModerator_Success() {
        authenticateAsAdmin();
        Long targetId = targetUser.getUserId();

        ResponseEntity<UserDto> response = roleAdminController.assignRole(targetId, "ROLE_MODERATOR");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDto updated = response.getBody();
        assertNotNull(updated);
        assertTrue(updated.getRoles().contains("ROLE_MODERATOR"));

        // 🔧 Исправленная проверка email:
        verify(emailService).sendHtmlEmail(
                eq("target@test.com"),
                eq("Вам назначена новая роль на LitSite"),  // 🔧 Точный предмет
                argThat(html -> html.contains("MODERATOR") || html.contains("Модерация"))  // 🔧 Гибкая проверка
        );
    }

    @Test
    @Transactional
    @DisplayName("PUT /admin/users/{userId}/role: назначение роли админа")
    void assignRole_ToAdmin_Success() {
        authenticateAsAdmin();
        Long targetId = targetUser.getUserId();

        ResponseEntity<UserDto> response = roleAdminController.assignRole(targetId, "ROLE_ADMIN");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getRoles().contains("ROLE_ADMIN"));

        verify(emailService).sendHtmlEmail(
                anyString(), anyString(),
                argThat(html -> html.contains("Полный доступ к управлению"))
        );
    }

    @Test
    @Transactional
    @DisplayName("PUT /admin/users/{userId}/role: не-админ получает 403")
    void assignRole_NonAdmin_Forbidden() {
        authenticateAsRegular();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.assignRole(targetUser.getUserId(), "ROLE_MODERATOR")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    @DisplayName("PUT /admin/users/{userId}/role: несуществующий пользователь")
    void assignRole_UserNotFound() {
        authenticateAsAdmin();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.assignRole(999L, "ROLE_MODERATOR")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @Transactional
    @DisplayName("PUT /admin/users/{userId}/role: несуществующая роль")
    void assignRole_RoleNotFound() {
        authenticateAsAdmin();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.assignRole(targetUser.getUserId(), "ROLE_SUPERADMIN")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ===== Тесты DELETE /admin/users/{userId}/role =====

    @Test
    @Transactional
    @DisplayName("DELETE /admin/users/{userId}/role: админ снимает роль модератора")
    void removeRole_Admin_Success() {
        authenticateAsAdmin();

        // Даём целевому пользователю роль модератора
        targetUser.addRole(moderatorRole);
        targetUser = userRepository.save(targetUser);

        ResponseEntity<UserDto> response = roleAdminController.removeRole(
                targetUser.getUserId(), "ROLE_MODERATOR"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getRoles().contains("ROLE_MODERATOR"));
    }

    @Test
    @Transactional
    @DisplayName("DELETE /admin/users/{userId}/role: нельзя снять последнюю роль админа")
    void removeRole_LastAdmin_Protected() {
        authenticateAsAdmin();

        // 🔧 Гарантируем, что targetUser — ЕДИНСТВЕННЫЙ админ
        // Удаляем роль админа у adminUser, чтобы остался только targetUser
        adminUser.getRoles().remove(adminRole);
        adminUser = userRepository.save(adminUser);

        // Даём targetUser роль админа
        targetUser.getRoles().clear();
        targetUser.addRole(adminRole);
        targetUser = userRepository.save(targetUser);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.removeRole(targetUser.getUserId(), "ROLE_ADMIN")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("последнюю роль админа"));
    }

    @Test
    @Transactional
    @DisplayName("DELETE /admin/users/{userId}/role: можно снять роль, если есть другие админы")
    void removeRole_NotLastAdmin_Success() {
        authenticateAsAdmin();

        // Есть два админа
        regularUser.addRole(adminRole);
        regularUser = userRepository.save(regularUser);

        targetUser.getRoles().clear();
        targetUser.addRole(adminRole);
        targetUser = userRepository.save(targetUser);

        ResponseEntity<UserDto> response = roleAdminController.removeRole(
                targetUser.getUserId(), "ROLE_ADMIN"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getRoles().contains("ROLE_ADMIN"));
    }

    @Test
    @Transactional
    @DisplayName("DELETE /admin/users/{userId}/role: нельзя снять единственную роль ROLE_USER")
    void removeRole_LastUserRole_Protected() {
        authenticateAsAdmin();

        // У пользователя только ROLE_USER
        targetUser.getRoles().clear();
        targetUser.addRole(userRole);
        targetUser = userRepository.save(targetUser);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.removeRole(targetUser.getUserId(), "ROLE_USER")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("хотя бы одна роль"));
    }

    @Test
    @Transactional
    @DisplayName("DELETE /admin/users/{userId}/role: не-админ получает 403")
    void removeRole_NonAdmin_Forbidden() {
        authenticateAsRegular();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.removeRole(targetUser.getUserId(), "ROLE_USER")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ===== Тесты GET /admin/roles =====

    @Test
    @DisplayName("GET /admin/roles: админ получает все роли")
    void getAllRoles_Admin_Success() {
        authenticateAsAdmin();

        List<Role> roles = roleAdminController.getAllRoles();

        assertNotNull(roles);
        assertEquals(3, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("GET /admin/roles: не-админ получает 403")
    void getAllRoles_NonAdmin_Forbidden() {
        authenticateAsRegular();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.getAllRoles()
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ===== Тесты POST /admin/roles =====

    @Test
    @Transactional
    @DisplayName("POST /admin/roles: админ создаёт новую роль")
    void createRole_Admin_Success() {
        authenticateAsAdmin();
        Role newRole = new Role();
        newRole.setName("ROLE_EDITOR");

        var response = roleAdminController.createRole(newRole);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Role created = response.getBody();
        assertNotNull(created);
        assertEquals("ROLE_EDITOR", created.getName());
        assertTrue(roleRepository.findByName("ROLE_EDITOR").isPresent());
    }

    @Test
    @Transactional
    @DisplayName("POST /admin/roles: создание дубликата роли")
    void createRole_Duplicate_Conflict() {
        authenticateAsAdmin();
        Role duplicateRole = new Role();
        duplicateRole.setName("ROLE_ADMIN");  // Уже существует

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.createRole(duplicateRole)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("уже существует"));
    }

    @Test
    @Transactional
    @DisplayName("POST /admin/roles: не-админ получает 403")
    void createRole_NonAdmin_Forbidden() {
        authenticateAsRegular();
        Role newRole = new Role();
        newRole.setName("ROLE_EDITOR");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roleAdminController.createRole(newRole)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}