package com.litsite.lit.controller;

import com.litsite.lit.dto.AddBookDto;
import com.litsite.lit.dto.RegisterUserDto;
import com.litsite.lit.dto.UpdateProfileDescriptionDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.security.CustomUserDetails;
import com.litsite.lit.security.CustomUserService;
import com.litsite.lit.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Transactional
    @GetMapping("/me")
    public ResponseEntity<MyUser> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        MyUser user = userService.getUser(currentUser.user().getEmail());
        return ResponseEntity.ok(user);
    }


    @PutMapping("/me/description")
    @Transactional
    public ResponseEntity<MyUser> updateProfileDescription(@RequestBody UpdateProfileDescriptionDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        MyUser user = userService.updateProfileDescription(currentUser.getUsername(), dto.getProfileDescription());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/me/addwork")
    @Transactional
    public ResponseEntity<Integer> addWork(@RequestBody AddBookDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        MyUser user = userService.getUser(currentUser.getUsername());

        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setDescription(dto.getDescription());
        book.setPublicationDate(LocalDateTime.now());
        book.setViewsAmount(0);
        book.setUser(user);

        Book savedBook = userService.saveBook(book); // сохраняем напрямую

        return ResponseEntity.ok(savedBook.getBookId()); // id уже есть
    }


    @GetMapping
    public ResponseEntity<List<MyUser>> allUsers() {
        List <MyUser> users = userService.allUsers();
        return ResponseEntity.ok(users);
    }
}
