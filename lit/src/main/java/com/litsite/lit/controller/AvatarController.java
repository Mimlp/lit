package com.litsite.lit.controller;

import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.FileStorageService;
import com.litsite.lit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AvatarController {

    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final AuthHelper authHelper;
    private final UserMapper userMapper;

    @PostMapping("/user/me/avatar")
    public ResponseEntity<UserDto> uploadAvatar(@RequestParam("file") MultipartFile file) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();

        if (currentUser.getAvatarUrl() != null) {
            fileStorageService.deleteAvatar(currentUser.getAvatarUrl());
        }

        String avatarUrl = fileStorageService.saveAvatar(file, currentUser.getUserId());
        currentUser.setAvatarUrl(avatarUrl);

        MyUser updated = userService.save(currentUser);
        return ResponseEntity.ok(userMapper.toDto(updated));
    }

    @DeleteMapping("/user/me/avatar")
    public ResponseEntity<UserDto> deleteAvatar() {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();

        if (currentUser.getAvatarUrl() != null) {
            fileStorageService.deleteAvatar(currentUser.getAvatarUrl());
            currentUser.setAvatarUrl(null);
            MyUser updated = userService.save(currentUser);
            return ResponseEntity.ok(userMapper.toDto(updated));
        }

        return ResponseEntity.ok(userMapper.toDto(currentUser));
    }

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        return fileStorageService.serveAvatar(filename);
    }
}