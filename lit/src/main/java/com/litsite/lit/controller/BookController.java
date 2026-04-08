package com.litsite.lit.controller;

import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.TagDto;
import com.litsite.lit.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequestMapping
@RestController
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    @GetMapping("/books/{id}")
    public BookDto getBookById(@PathVariable int id) {
        BookDto book = bookService.getBookById(id);
        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        return book;
    }
    @GetMapping("/books")
    public List<BookDto> getBooks() {
        return bookService.getAllBooks();
    }
    @GetMapping("/users/me/books")
    public List<BookDto> getMyBooks() {
        return bookService.getMyBooks();
    }
    @GetMapping("/users/{userId}/books")
    public List<BookDto> getBooksByUserId(@PathVariable int userId) {
        return bookService.getBooksByUserId(userId);
    }
    @PutMapping("/books/{id}")
    public BookDto updateBook(@PathVariable int id, @RequestBody BookDto bookDto) {
        return bookService.updateBook(id, bookDto);
    }
    @GetMapping("/tags")
    public List<TagDto> getTags() {
        return bookService.getAllTags();
    }
}
