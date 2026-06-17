package com.litsite.lit.booktest;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.controller.BookController;
import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.dto.TagDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Tag;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.TagRepository;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.EmailService;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class BookControllerTest {

    // 🔧 ТЕСТОВАЯ КОНФИГУРАЦИЯ: подменяем AuthHelper и EmailService на моки
    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public AuthHelper authHelper() {
            return mock(AuthHelper.class);
        }

        @Bean @Primary
        public EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    @Autowired private BookController bookController;
    @Autowired private BookRepository bookRepository;
    @Autowired private EmailService emailService;  // Это теперь МОК
    @Autowired private UserRepository userRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private AuthHelper authHelper;  // Это теперь МОК

    private MyUser testUser;
    private MyUser otherUser;
    private MyUser moderatorUser;
    private Book testBook1;
    private Book testBook2;
    private Book testBook3;
    private Tag testTag1;
    private Tag testTag2;

    @BeforeEach
    public void before() {
        // Создаём пользователей
        testUser = createUser("testuser", "test@example.com");
        otherUser = createUser("otheruser", "other@example.com");
        moderatorUser = createUser("moderator", "mod@example.com");

        // Создаём теги
        testTag1 = createTag("Fantasy");
        testTag2 = createTag("Adventure");

        // 🔧 Создаём книги с МУТИРУЕМЫМИ коллекциями тегов (важно для Hibernate!)
        testBook1 = createBook("Book 1", "Description for Book 1", testUser, Set.of(testTag1));
        testBook2 = createBook("Book 2", "Description for Book 2", testUser, Set.of(testTag2));
        testBook3 = createBook("Book 3", "Description for Book 3", testUser, Set.of(testTag1, testTag2));

        // 🔧 БАЗОВЫЕ МОКИ для AuthHelper (владелец = testUser)
        lenient().when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        lenient().when(authHelper.getCurrentUserId()).thenReturn(testUser.getUserId());
        lenient().when(authHelper.isOwner(eq(testUser.getUserId()))).thenReturn(true);
        lenient().when(authHelper.isOwner(eq(otherUser.getUserId()))).thenReturn(false);
        lenient().when(authHelper.isAdminOrModerator()).thenReturn(false);
        lenient().when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(true);
        lenient().when(authHelper.isOwnerOrHasRole(eq(otherUser.getUserId()), any(), any())).thenReturn(false);
        lenient().doNothing().when(emailService).sendContentModerationNotice(anyString(), anyString(), anyString(), anyString());
    }

    @AfterEach
    public void after() {
        reset(authHelper, emailService);  // 🔧 Сбрасываем моки между тестами
        bookRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ===== Вспомогательные методы =====

    private MyUser createUser(String username, String email) {
        MyUser user = new MyUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed");
        user.setRegistrationDate(LocalDateTime.now());
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Tag createTag(String name) {
        Tag tag = new Tag();
        tag.setTagName(name);
        return tagRepository.save(tag);
    }

    private Book createBook(String title, String desc, MyUser owner, Set<Tag> tags) {
        Book book = new Book();
        book.setTitle(title);
        book.setDescription(desc);
        book.setUser(owner);

        // 🔧 КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: конвертируем в mutable HashSet для Hibernate!
        // Set.of() возвращает неизменяемую коллекцию, которую нельзя модифицировать
        book.setTags(new HashSet<>(tags));

        book.setPublicationDate(LocalDateTime.now());
        return bookRepository.save(book);
    }

    // ===== Базовые тесты =====

    @Test
    @Transactional
    void testGetBooks() {
        List<BookSimpleDto> books = bookController.getBooks();
        assertEquals(3, books.size());
    }

    @Test
    void testGetBookById() {
        BookDto book = bookController.getBookById(testBook1.getBookId());
        assertNotNull(book);
        assertEquals("Book 1", book.getTitle());
    }

    @Test
    void testGetTags() {
        var tags = bookController.getTags();
        assertEquals(2, tags.size());
    }

    @Test
    void testGetBookById_NotFound() {
        assertThrows(ResponseStatusException.class, () -> bookController.getBookById(999L));
    }

    // ===== Поиск =====

    @Test
    @Transactional
    void testSearchBooksByKeyword() {
        List<BookSimpleDto> books = bookController.searchBooks("Book 1", null, null);
        assertEquals(1, books.size());
    }

    @Test
    @Transactional
    void testSearchBooks_NoResults() {
        assertTrue(bookController.searchBooks("NonExistent", null, null).isEmpty());
    }

    @Test
    @Transactional
    void testSearchBooks_BlankKeyword_TreatedAsNull() {
        assertEquals(3, bookController.searchBooks("   ", null, null).size());
    }

    @Test
    @Transactional
    void testSearchBooks_CaseInsensitive() {
        assertEquals(1, bookController.searchBooks("book 1", null, null).size());
    }

    @Test
    @Transactional
    void testSearchBooks_PartialMatch() {
        assertEquals(3, bookController.searchBooks("Book", null, null).size());
    }

    @Test
    @Transactional
    void testSearchBooks_ByIncludeTag_Single() {
        List<BookSimpleDto> books = bookController.searchBooks(null, Set.of(testTag1.getTagId()), null);
        assertEquals(2, books.size());
    }

    @Test
    @Transactional
    void testSearchBooks_ByIncludeTags_Multiple_AndLogic() {
        List<BookSimpleDto> books = bookController.searchBooks(null, Set.of(testTag1.getTagId(), testTag2.getTagId()), null);
        assertEquals(1, books.size());
        assertEquals("Book 3", books.get(0).getTitle());
    }

    @Test
    @Transactional
    void testSearchBooks_ByIncludeTags_NoMatch() {
        assertTrue(bookController.searchBooks(null, Set.of(999L), null).isEmpty());
    }

    @Test
    @Transactional
    void testSearchBooks_EmptyIncludeTagSet_TreatedAsNull() {
        assertEquals(3, bookController.searchBooks("Book", Set.of(), null).size());
    }

    @Test
    @Transactional
    void testSearchBooks_ByExcludeTag_Single() {
        List<BookSimpleDto> books = bookController.searchBooks(null, null, Set.of(testTag1.getTagId()));
        assertEquals(1, books.size());
        assertEquals("Book 2", books.get(0).getTitle());
    }

    @Test
    @Transactional
    void testSearchBooks_ByExcludeTags_Multiple() {
        assertTrue(bookController.searchBooks(null, null, Set.of(testTag1.getTagId(), testTag2.getTagId())).isEmpty());
    }

    @Test
    @Transactional
    void testSearchBooks_KeywordAndIncludeTag_Match() {
        assertEquals(1, bookController.searchBooks("Book 1", Set.of(testTag1.getTagId()), null).size());
    }

    @Test
    @Transactional
    void testSearchBooks_KeywordAndIncludeTag_NoMatch() {
        assertTrue(bookController.searchBooks("Book 1", Set.of(testTag2.getTagId()), null).isEmpty());
    }

    @Test
    @Transactional
    void testSearchBooks_KeywordAndExcludeTag() {
        List<BookSimpleDto> books = bookController.searchBooks("Book", null, Set.of(testTag1.getTagId()));
        assertEquals(1, books.size());
        assertEquals("Book 2", books.get(0).getTitle());
    }

    @Test
    @Transactional
    void testSearchBooks_IncludeAndExcludeTags() {
        List<BookSimpleDto> books = bookController.searchBooks(null, Set.of(testTag2.getTagId()), Set.of(testTag1.getTagId()));
        assertEquals(1, books.size());
        assertEquals("Book 2", books.get(0).getTitle());
    }

    @Test
    @Transactional
    void testSearchBooks_IncludeAndExcludeTags_NoResults() {
        assertTrue(bookController.searchBooks(null, Set.of(testTag1.getTagId()), Set.of(testTag1.getTagId())).isEmpty());
    }

    // ===== По пользователю =====

    @Test
    @Transactional
    void testGetBooksByUserId() {
        List<BookSimpleDto> books = bookController.getBooksByUserId(testUser.getUserId());
        assertEquals(3, books.size());
    }

    @Test
    @Transactional
    void testGetBooksByUserId_NoBooks() {
        assertTrue(bookController.getBooksByUserId(otherUser.getUserId()).isEmpty());
    }

    // ===== Теги =====

    @Test
    void testAddNewTag_Success() {
        TagDto dto = new TagDto();
        dto.setTagName("NewTag");
        TagDto created = bookController.addNewTag(dto);
        assertNotNull(created.getTagId());
        assertEquals("NewTag", created.getTagName());
    }

    @Test
    void testAddNewTag_Duplicate() {
        TagDto dto = new TagDto();
        dto.setTagName("Fantasy");
        assertThrows(ResponseStatusException.class, () -> bookController.addNewTag(dto));
    }

    @Test
    @Transactional
    void testSearchBooks_SpecialCharacters() {
        assertNotNull(bookController.searchBooks("Book%", null, null));
    }

    // ===== Удаление книги =====

    @Test
    @Transactional
    void testDeleteBook_Success_ByOwner() {
        // Владелец удаляет свою книгу — мок уже настроен в @BeforeEach
        assertDoesNotThrow(() -> bookController.deleteBook(testBook1.getBookId(), null));
        assertFalse(bookRepository.existsById(testBook1.getBookId()));
        verify(emailService, never()).sendContentModerationNotice(any(), any(), any(), any());
    }

    @Test
    @Transactional
    void testDeleteBook_ByModerator_WithReason() {
        // 🔧 Переопределяем мок для сценария модератора
        when(authHelper.getCurrentUserOrThrow()).thenReturn(moderatorUser);
        when(authHelper.getCurrentUserId()).thenReturn(moderatorUser.getUserId());
        when(authHelper.isAdminOrModerator()).thenReturn(true);
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(true);

        assertDoesNotThrow(() -> bookController.deleteBook(testBook1.getBookId(), "Нарушение правил"));
        assertFalse(bookRepository.existsById(testBook1.getBookId()));
        verify(emailService).sendContentModerationNotice(
                eq("test@example.com"), eq("книга"), eq("Book 1"), eq("Нарушение правил")
        );
    }

    @Test
    @Transactional
    void testDeleteBook_Forbidden() {
        // 🔧 Другой пользователь без прав
        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.getCurrentUserId()).thenReturn(otherUser.getUserId());
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookController.deleteBook(testBook1.getBookId(), null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ===== Обновление книги =====

    @Test
    @Transactional
    void testUpdateBook_Success() {
        BookDto dto = new BookDto();
        dto.setTitle("Updated");
        dto.setDescription("New desc");
        BookDto result = bookController.updateBook(testBook1.getBookId(), dto);
        assertNotNull(result);
        assertEquals("Updated", result.getTitle());
    }

    @Test
    @Transactional
    void testUpdateBook_Forbidden() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookController.updateBook(testBook1.getBookId(), new BookDto()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ===== Обновление тегов =====

    @Test
    @Transactional
    void testUpdateBookTags_Success() {
        Set<Long> newTags = Set.of(testTag1.getTagId(), testTag2.getTagId());
        BookDto result = bookController.updateBookTags(testBook1.getBookId(), newTags);
        assertNotNull(result);
        assertEquals(2, result.getTags().size());
    }

    @Test
    @Transactional
    void testUpdateBookTags_Forbidden() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookController.updateBookTags(testBook1.getBookId(), Set.of(testTag1.getTagId())));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ===== Мои книги =====

    @Test
    @Transactional
    void testGetMyBooks_Success() {
        // Мок уже настроен: getCurrentUserId() → testUser.getUserId()
        List<BookSimpleDto> books = bookController.getMyBooks();
        assertEquals(3, books.size());
    }

    @Test
    void testGetMyBooks_Unauthenticated() {
        when(authHelper.getCurrentUserId()).thenReturn(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> bookController.getMyBooks());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}