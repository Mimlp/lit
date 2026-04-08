package com.litsite.lit.service;

import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.TagDto;
import com.litsite.lit.mapper.BookMapper;
import com.litsite.lit.mapper.TagMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Tag;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.TagRepository;
import com.litsite.lit.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final TagMapper tagMapper;
    private final TagRepository tagRepository;
    @Transactional
    public BookDto getBookById(int id) {
        Book book = bookRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        return bookMapper.bookToBookDto(book, getCurrentUserId());
    }

    public List<BookDto> getAllBooks() {
        Integer currentUserId = getCurrentUserId();
        return bookRepository.findAll().stream()
                .map(b -> bookMapper.bookToBookDto(b, currentUserId))
                .toList();
    }

    public List<BookDto> getMyBooks() {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        return bookRepository.findByUserUserIdOrderByPublicationDateDesc(userId).stream()
                .map(book -> bookMapper.bookToBookDto(book, userId))
                .toList();
    }

    public List<BookDto> getBooksByUserId(int userId) {
        Integer currentUserId = getCurrentUserId();
        return bookRepository.findByUserUserIdOrderByPublicationDateDesc(userId).stream()
                .map(b -> bookMapper.bookToBookDto(b, currentUserId))
                .toList();
    }

    public BookDto updateBook(int id, BookDto bookDto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        book.setTitle(bookDto.getTitle());
        book.setDescription(bookDto.getDescription());
        // При обновлении права можно не пересчитывать, либо вернуть как выше
        return bookMapper.bookToBookDto(bookRepository.save(book), getCurrentUserId());
    }

    // 🔹 Единый безопасный метод получения ID
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.user().getUserId();
        }
        return null; // Анонимный пользователь
    }

    private MyUser getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.user();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }

    public List<TagDto> getAllTags() {
        return tagMapper.tagToTagDtoList(tagRepository.findAll());
    }
}