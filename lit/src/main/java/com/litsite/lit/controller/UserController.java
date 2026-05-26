package com.litsite.lit.controller;

import com.litsite.lit.dto.AddBookDto;
import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.UpdateProfileDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthHelper authHelper;

    @Transactional
    @GetMapping("/me")
    public ResponseEntity<UserDto> authenticatedUser() {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        return ResponseEntity.ok(userMapper.toDto(currentUser));
    }

    @PutMapping("/me/profile")
    @Transactional
    public ResponseEntity<UserDto> updateProfile(@RequestBody UpdateProfileDto updateProfileDto) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        MyUser updated = userService.updateProfile(currentUser.getEmail(), updateProfileDto);
        return ResponseEntity.ok(userMapper.toDto(updated));
    }

    @PostMapping("/me/addwork")
    @Transactional
    public ResponseEntity<Long> addWork(@RequestBody AddBookDto dto) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();

        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setDescription(dto.getDescription());
        book.setPublicationDate(LocalDateTime.now());
        book.setViewsAmount(0);
        book.setUser(currentUser);

        Book savedBook = userService.saveBook(book);
        return ResponseEntity.ok(savedBook.getBookId());
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> allUsers() {
        List<UserDto> users = userMapper.toDto(userService.allUsers());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getAuthor(id));
    }
}