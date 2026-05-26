package com.litsite.lit.service;

import com.litsite.lit.dto.BooklistDto;
import com.litsite.lit.mapper.BooklistMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.BookList;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.BooklistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BooklistService {
    private final BooklistRepository booklistRepository;
    private final BooklistMapper booklistMapper;
    private final BookRepository bookRepository;

    @Transactional
    public List<BooklistDto> findByUserId(Long userId) {
        return booklistMapper.bookListToBooklistDto(booklistRepository.findByUserUserId(userId));
    }

    public BooklistDto createBooklist(String title, MyUser user) {
        BookList bookList = new BookList();
        bookList.setTitle(title);
        bookList.setCreationDate(LocalDateTime.now());
        bookList.setUser(user);
        return booklistMapper.bookListToBooklistDto(booklistRepository.save(bookList));
    }

    @Transactional
    public BooklistDto addBookToList(Long booklistId, Long bookId, MyUser user) {
        BookList bookList = booklistRepository.findById(booklistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Список не найден"));

        if (!bookList.getUser().getUserId().equals(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав для изменения списка");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

        if (bookList.getBooks().add(book)) {
            book.getBookLists().add(bookList);
            booklistRepository.save(bookList);
        }

        return booklistMapper.bookListToBooklistDto(bookList);
    }

    @Transactional
    public void removeBookFromList(Long booklistId, Long bookId, MyUser user) {
        BookList bookList = booklistRepository.findById(booklistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Список не найден"));

        if (!bookList.getUser().getUserId().equals(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав для изменения списка");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

        bookList.getBooks().remove(book);
        book.getBookLists().remove(bookList);
    }

    @Transactional
    public BooklistDto getById(Long listId) {
        BookList bookList = booklistRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Список не найден"));
        return booklistMapper.bookListToBooklistDto(bookList);
    }

    @Transactional
    public BooklistDto updateBooklist(Long listId, BooklistDto dto, MyUser currentUser) {
        BookList bookList = booklistRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Список не найден"));

        if (!bookList.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Только владелец может редактировать список");
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            bookList.setTitle(dto.getTitle());
        }

        BookList saved = booklistRepository.save(bookList);
        return booklistMapper.bookListToBooklistDto(saved);
    }

    @Transactional
    public void deleteBooklist(Long listId, MyUser currentUser) {
        BookList bookList = booklistRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Список не найден"));

        if (!bookList.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Только владелец может удалять список");
        }
        booklistRepository.delete(bookList);
    }
}
