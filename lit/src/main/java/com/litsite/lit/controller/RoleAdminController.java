package com.litsite.lit.controller;

import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Role;
import com.litsite.lit.service.EmailService;
import com.litsite.lit.service.RoleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;
    private final AuthHelper authHelper;
    private final EmailService emailService;
    private final UserMapper userMapper;

    @GetMapping("/roles/available")
    public ResponseEntity<List<String>> getAvailableRoles() {
        if (!authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(List.of("ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN"));
    }

    @PutMapping("/users/{userId}/role")
    @Transactional
    public ResponseEntity<UserDto> assignRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {

        if (!authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if ("ROLE_ADMIN".equals(roleName) && !authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Только админ может назначать роль админа");
        }

        MyUser targetUser = roleService.assignRole(userId, roleName);

        // 📧 Уведомление
        if (targetUser.getEmail() != null && !"ROLE_USER".equals(roleName)) {
            try {
                emailService.sendHtmlEmail(
                        targetUser.getEmail(),
                        "Вам назначена новая роль на LitSite",
                        String.format("""
                        <html><body>
                        <h2>Здравствуйте, %s!</h2>
                        <p>Администратор назначил вам роль <strong>%s</strong>.</p>
                        <p><strong>Новые возможности:</strong> %s</p>
                        <hr/><p style="font-size:12px;color:#666">Команда LitSite</p>
                        </body></html>
                        """,
                                targetUser.getUsername(),
                                roleName.replace("ROLE_", ""),
                                "ROLE_MODERATOR".equals(roleName)
                                        ? "Модерация контента"
                                        : "Полный доступ к управлению"
                        )
                );
            } catch (Exception e) {
                System.err.println("Failed to send role email: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(userMapper.toDto(targetUser));
    }

    // ✅ DELETE /admin/users/{userId}/role — удалить роль
    @DeleteMapping("/users/{userId}/role")
    @Transactional
    public ResponseEntity<UserDto> removeRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {

        if (!authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // 🔐 Защита: нельзя снять последнюю роль админа
        if ("ROLE_ADMIN".equals(roleName)) {
            long adminCount = roleService.countUsersWithRole("ROLE_ADMIN");
            if (adminCount <= 1) {
                MyUser target = roleService.getUserById(userId);
                if (target != null && target.hasRole("ROLE_ADMIN")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя удалить последнюю роль админа");
                }
            }
        }

        MyUser updated = roleService.removeRole(userId, roleName);
        return ResponseEntity.ok(userMapper.toDto(updated));
    }

    // ✅ GET /admin/roles — все роли (для админ-панели)
    @GetMapping("/roles")
    public List<Role> getAllRoles() {
        if (!authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return roleService.getAllRoles();
    }

    // ✅ POST /admin/roles — создать новую роль
    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        if (!authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(role.getName()));
    }
}