package com.litsite.lit;

import com.litsite.lit.controller.BookController;
import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Tag;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.TagRepository;
import com.litsite.lit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookControllerTest {

    @Autowired
    private BookController bookController;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    private MyUser testUser;
    private Book testBook1;
    private Book testBook2;
    private Tag testTag1;
    private Tag testTag2;

    @BeforeEach
    public void before() {
        testUser = new MyUser();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("password123");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        testTag1 = new Tag();
        testTag1.setTagName("Fantasy");
        testTag1 = tagRepository.save(testTag1);

        testTag2 = new Tag();
        testTag2.setTagName("Adventure");
        testTag2 = tagRepository.save(testTag2);

        testBook1 = new Book();
        testBook1.setTitle("Book 1");
        testBook1.setDescription("Description for Book 1");
        testBook1.setUser(testUser);
        testBook1.setTags(Set.of(testTag1));
        testBook1.setPublicationDate(LocalDateTime.now());
        testBook1 = bookRepository.save(testBook1);

        testBook2 = new Book();
        testBook2.setTitle("Book 2");
        testBook2.setDescription("Description for Book 2");
        testBook2.setUser(testUser);
        testBook2.setTags(Set.of(testTag2));
        testBook2.setPublicationDate(LocalDateTime.now());
        testBook2 = bookRepository.save(testBook2);
    }

    @Test
    void testGetBookById() {
        BookDto book = bookController.getBookById(testBook1.getBookId());

        assertNotNull(book);
        assertEquals(testBook1.getBookId(), book.getBookId());
        assertEquals("Book 1", book.getTitle());
        assertEquals("Description for Book 1", book.getDescription());
    }

    @Test
    void testSearchBooksByKeyword() {
        List<BookSimpleDto> books = bookController.searchBooks("Book 1", null);

        assertEquals(1, books.size());
        assertEquals("Book 1", books.get(0).getTitle());
    }

    @Test
    void testSearchBooksByTag() {
        List<BookSimpleDto> books = bookController.searchBooks(null, Set.of(testTag1.getTagId()));

        assertEquals(1, books.size());
        assertEquals("Book 1", books.get(0).getTitle());
    }

    @Test
    void testGetTags() {
        var tags = bookController.getTags();

        assertEquals(2, tags.size());
    }
}