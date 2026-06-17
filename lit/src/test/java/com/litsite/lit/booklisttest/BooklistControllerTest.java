package com.litsite.lit.booklisttest;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.controller.BooklistController;
import com.litsite.lit.dto.BooklistDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.BookList;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.BooklistRepository;
import com.litsite.lit.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class BooklistControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public AuthHelper authHelper() { return mock(AuthHelper.class); }
    }

    @Autowired private BooklistController booklistController;
    @Autowired private BooklistRepository booklistRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthHelper authHelper;

    private MyUser testUser;
    private BookList testBookList1;
    private BookList testBookList2;
    private Book testBook1;

    @BeforeEach void before() {
        lenient().when(authHelper.getCurrentUserOrThrow()).thenAnswer(inv -> testUser);
        lenient().when(authHelper.getCurrentUserId()).thenReturn(testUser != null ? testUser.getUserId() : 1L);

        testUser = new MyUser();
        testUser.setUsername("listuser"); testUser.setEmail("list@example.com");
        testUser.setPasswordHash("pass"); testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        testBook1 = new Book(); testBook1.setTitle("Book for List"); testBook1.setPublicationDate(LocalDateTime.now());
        testBook1 = bookRepository.save(testBook1);

        testBookList1 = new BookList(); testBookList1.setTitle("List 1"); testBookList1.setCreationDate(LocalDateTime.now());
        testBookList1.setUser(testUser);
        testBookList1.setBooks(new HashSet<>()); testBookList1.getBooks().add(testBook1);
        testBookList1 = booklistRepository.save(testBookList1);

        testBookList2 = new BookList(); testBookList2.setTitle("List 2"); testBookList2.setCreationDate(LocalDateTime.now());
        testBookList2.setUser(testUser); testBookList2.setBooks(new HashSet<>());
        testBookList2 = booklistRepository.save(testBookList2);
    }

    @AfterEach void after() {
        booklistRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test @Transactional void testFindByUser_Success() {
        List<BooklistDto> lists = booklistController.findByUser(testUser.getUserId());
        assertEquals(2, lists.size());
    }

    @Test @Transactional void testGetBooklistById_Success() {
        BooklistDto res = booklistController.getBooklistById(testBookList1.getListId());
        assertEquals(testBookList1.getListId(), res.getListId());
    }

    @Test void testGetBooklistById_NotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> booklistController.getBooklistById(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test @Transactional void testCreateBooklist_Success() {
        BooklistDto dto = new BooklistDto(); dto.setTitle("New");
        BooklistDto created = booklistController.createBooklist(dto);
        assertNotNull(created.getListId());
        assertEquals("New", created.getTitle());
    }

    @Test @Transactional void testUpdateBooklist_Success() {
        BooklistDto dto = new BooklistDto(); dto.setTitle("Updated");
        BooklistDto updated = booklistController.updateBooklist(testBookList1.getListId(), dto);
        assertEquals("Updated", updated.getTitle());
    }

    @Test void testUpdateBooklist_NotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> booklistController.updateBooklist(999L, new BooklistDto()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test @Transactional void testDeleteBooklist_Success() {
        booklistController.deleteBooklist(testBookList1.getListId());
        assertThrows(ResponseStatusException.class, () -> booklistController.getBooklistById(testBookList1.getListId()));
    }

    @Test void testDeleteBooklist_NotFound() {
        assertThrows(ResponseStatusException.class, () -> booklistController.deleteBooklist(999L));
    }

    @Test @Transactional void testAddBookToList_Success() {
        Book newBook = new Book(); newBook.setTitle("B"); newBook.setUser(testUser); newBook.setPublicationDate(LocalDateTime.now());
        newBook = bookRepository.save(newBook);
        BooklistDto res = booklistController.addBookToList(testBookList2.getListId(), newBook.getBookId());
        assertEquals(1, res.getBooks().size());
    }

    @Test void testAddBookToList_BooklistNotFound() {
        assertThrows(ResponseStatusException.class, () -> booklistController.addBookToList(999L, 1L));
    }

    @Test @Transactional void testRemoveBookFromList_Success() {
        booklistController.removeBookFromList(testBookList1.getListId(), testBook1.getBookId());
        BooklistDto res = booklistController.getBooklistById(testBookList1.getListId());
        assertTrue(res.getBooks().isEmpty());
    }

    @Test void testRemoveBookFromList_BooklistNotFound() {
        assertThrows(ResponseStatusException.class, () -> booklistController.removeBookFromList(999L, 1L));
    }
}