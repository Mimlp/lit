package com.litsite.lit.controller;

import com.litsite.lit.dto.RegisterUserDto;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.security.CustomUserDetails;
import com.litsite.lit.security.CustomUserService;
import com.litsite.lit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<RegisterUserDto> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        RegisterUserDto rud = new RegisterUserDto();
        rud.setEmail(currentUser.user().getEmail());
        rud.setLogin(currentUser.user().getLogin());
        rud.setPassword(currentUser.getPassword());
        rud.setUsername(currentUser.getUsername());
        return ResponseEntity.ok(rud);
    }

    @GetMapping
    public ResponseEntity<List<MyUser>> allUsers() {
        List <MyUser> users = userService.allUsers();
        return ResponseEntity.ok(users);
    }
}
