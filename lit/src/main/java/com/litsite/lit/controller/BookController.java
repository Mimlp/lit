package com.litsite.lit.controller;

import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.BookFilterRequest;
import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.dto.TagDto;
<<<<<<< Updated upstream
import com.litsite.lit.models.MyUser;
=======
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
>>>>>>> Stashed changes
import com.litsite.lit.service.BookService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RequestMapping
@RestController
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final AuthHelper authHelper;
<<<<<<< Updated upstream
=======
    private final BookRepository bookRepository;
>>>>>>> Stashed changes
    @GetMapping("/books/{id}")
    public BookDto getBookById(@PathVariable long id) {
        BookDto book = bookService.getBookById(id);
        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        return book;
    }
    @GetMapping("/books")
    public List<BookSimpleDto> getBooks() {
        return bookService.getAllBooks();
    }
    @GetMapping("/users/me/books")
    public List<BookSimpleDto> getMyBooks() {
        return bookService.getMyBooks();
    }
    @GetMapping("/users/{userId}/books")
    public List<BookSimpleDto> getBooksByUserId(@PathVariable long userId) {
        return bookService.getBooksByUserId(userId);
    }
    @PutMapping("/books/{id}")
    public BookDto updateBook(@PathVariable long id, @RequestBody BookDto bookDto) {
        return bookService.updateBook(id, bookDto);
    }
    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
<<<<<<< Updated upstream
    public void deleteBook(@PathVariable long id) {
        bookService.deleteBook(id);
=======
    @Transactional
    public void deleteBook(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        Book book = bookRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!authHelper.isOwnerOrHasRole(book.getUser().getUserId(), "ROLE_MODERATOR", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        boolean isModeratorAction = !currentUser.getUserId().equals(book.getUser().getUserId());
        bookService.deleteBook(id, isModeratorAction ? reason : null);
>>>>>>> Stashed changes
    }
    @GetMapping("/tags")
    public List<TagDto> getTags() {
        return bookService.getAllTags();
    }
    @GetMapping("/books/search")
    public List<BookSimpleDto> searchBooks(
            @RequestParam(required = false) String keyword,
<<<<<<< Updated upstream
            @RequestParam(required = false) Set<Long> tagIds
    ) {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword(keyword);
        filter.setTagIds(tagIds);
=======
            @RequestParam(required = false) Set<Long> includeTagIds,
            @RequestParam(required = false) Set<Long> excludeTagIds
    ) {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword(keyword);
        filter.setIncludeTagIds(includeTagIds);
        filter.setExcludeTagIds(excludeTagIds);
>>>>>>> Stashed changes
        return bookService.findBooksByFilter(filter);
    }
    @PutMapping("/books/{bookId}/tags")
    public BookDto updateBookTags(
            @PathVariable Long bookId,
            @RequestBody Set<Long> tagIds
    ) {
        MyUser currentUser = authHelper.getCurrentUserOrThrow();
        return bookService.updateBookTags(bookId, tagIds, currentUser);
    }
    @PostMapping("/books/tags")
    public TagDto addNewTag(@RequestBody TagDto tagDto) {
        return bookService.addNewTag(tagDto);
    }

}
