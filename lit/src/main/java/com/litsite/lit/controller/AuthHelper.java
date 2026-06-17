package com.litsite.lit.controller;

import com.litsite.lit.models.MyUser;
import com.litsite.lit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

<<<<<<< Updated upstream
=======
import java.util.Objects;
>>>>>>> Stashed changes
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthHelper {
    public MyUser getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.user();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
    }

    public Long getCurrentUserId() {
        try {
            return getCurrentUserOrThrow().getUserId();
        } catch (ResponseStatusException e) {
            return null;
        }
    }

    public Optional<MyUser> getCurrentUserIfAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails details) {
            return Optional.of(details.user());
        }
        return Optional.empty();
    }
<<<<<<< Updated upstream
=======

    public boolean hasRole(String roleName) {
        try {
            return getCurrentUserOrThrow().hasRole(roleName);
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    // ✅ Проверка нескольких ролей
    public boolean hasAnyRole(String... roleNames) {
        try {
            return getCurrentUserOrThrow().hasAnyRole(roleNames);
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    // ✅ Удобные хелперы
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public boolean isModerator() {
        return hasRole("ROLE_MODERATOR");
    }

    public boolean isAdminOrModerator() {
        return hasAnyRole("ROLE_ADMIN", "ROLE_MODERATOR");
    }

    // ✅ В конец класса AuthHelper
    public boolean isOwner(Long resourceOwnerId) {
        try {
            MyUser user = getCurrentUserOrThrow();
            return user.getUserId().equals(resourceOwnerId);
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    public boolean isOwnerOrHasRole(Long resourceOwnerId, String... roleNames) {
        return isOwner(resourceOwnerId) || hasAnyRole(roleNames);
    }
>>>>>>> Stashed changes
}