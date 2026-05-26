package com.litsite.lit.controller;

import com.litsite.lit.dto.BooklistDto;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.BooklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequestMapping
@RestController
@RequiredArgsConstructor
public class BooklistController {
    private final BooklistService booklistService;
    private final AuthHelper authHelper;

    @GetMapping("/user/me/booklists")
    public List<BooklistDto> findByUserUserId() {
        Long userId = authHelper.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return booklistService.findByUserId(userId);
    }

    @GetMapping("/user/{userId}/booklists")
    public List<BooklistDto> findByUser(@PathVariable Long userId) {
        return booklistService.findByUserId(userId);
    }

    @PostMapping("/user/me/booklists")
    public BooklistDto createBooklist(@RequestBody BooklistDto booklistDto) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        return booklistService.createBooklist(booklistDto.getTitle(), currentUser);
    }

    @PutMapping("/user/me/booklists/{booklistId}/books/{bookId}")
    public BooklistDto addBookToList(@PathVariable Long booklistId, @PathVariable Long bookId) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        return booklistService.addBookToList(booklistId, bookId, currentUser);
    }

    @DeleteMapping("/user/me/booklists/{booklistId}/books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookFromList(@PathVariable Long booklistId, @PathVariable Long bookId) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        booklistService.removeBookFromList(booklistId, bookId, currentUser);
    }
    @GetMapping("/user/booklists/{listId}")
    public BooklistDto getBooklistById(@PathVariable Long listId) {
        return booklistService.getById(listId);
    }

    @PutMapping("/user/me/booklists/{listId}")
    public BooklistDto updateBooklist(
            @PathVariable Long listId,
            @RequestBody BooklistDto booklistDto
    ) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        return booklistService.updateBooklist(listId, booklistDto, currentUser);
    }

    @DeleteMapping("/user/me/booklists/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooklist(@PathVariable Long listId) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        booklistService.deleteBooklist(listId, currentUser);
    }
}