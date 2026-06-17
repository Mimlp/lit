package com.litsite.lit.controller;

import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.EmailService;
import com.litsite.lit.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final AuthHelper authHelper;
    private final EmailService emailService;
    private final UserMapper userMapper;

    @PutMapping("/{userId}/toggle-enabled")
    @Transactional
    public ResponseEntity<UserDto> toggleUserEnabled(
            @PathVariable Long userId,
            @RequestParam Boolean enabled,
            @RequestParam(required = false) String reason) {

        if (!authHelper.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        MyUser targetUser = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (Boolean.FALSE.equals(enabled) && targetUser.getEmail() != null) {
            try {
                emailService.sendAccountDisabledNotice(
                        targetUser.getEmail(),
                        targetUser.getUsername(),
                        reason != null ? reason : "Нарушение правил платформы"
                );
            } catch (Exception e) {
                System.err.println("Failed to send account disabled email: " + e.getMessage());
            }
        }

        targetUser.setEnabled(enabled);
        MyUser updated = userService.save(targetUser);

        return ResponseEntity.ok(userMapper.toDto(updated));
    }
}
