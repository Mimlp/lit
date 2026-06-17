package com.litsite.lit.chaptertest;

import com.litsite.lit.controller.ChapterController;
import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.exception.BookNotFoundException;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Chapter;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.ChapterRepository;
import com.litsite.lit.repository.UserRepository;  // 🔧 Добавляем
import com.litsite.lit.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;  // 🔧 Для пароля
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ChapterControllerTest {

    @Autowired private ChapterController chapterController;
    @Autowired private BookRepository bookRepository;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private UserRepository userRepository;  // 🔧 Инжектим репозиторий
    @Autowired private PasswordEncoder passwordEncoder;  // 🔧 Для кодирования пароля

    private Book testBook;
    private MyUser testUser;

    @BeforeEach
    void before() {
        // 🔧 1. Создаём и СОХРАНЯЕМ тестового пользователя
        testUser = new MyUser();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));  // 🔧 Не пустой пароль
        testUser.setEnabled(true);
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);  // 🔧 КЛЮЧЕВОЕ: сохраняем в БД!

        // 🔧 2. Создаём книгу с установленным владельцем (теперь он в БД!)
        testBook = new Book();
        testBook.setTitle("Test Book " + System.currentTimeMillis());
        testBook.setDescription("Desc");
        testBook.setPublicationDate(LocalDateTime.now());
        testBook.setUser(testUser);  // 🔧 Ссылка на сохранённого пользователя
        testBook = bookRepository.save(testBook);

        // 🔧 3. Устанавливаем SecurityContext с CustomUserDetails
        CustomUserDetails customUserDetails = new CustomUserDetails(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        customUserDetails,
                        null,
                        customUserDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void after() {
        SecurityContextHolder.clearContext();
        chapterRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();  // 🔧 Очищаем и пользователей
    }

    @Test
    void testGetChapters_BookNotFound() {
        assertThrows(BookNotFoundException.class, () -> chapterController.getChapters(999L));
    }

    @Test
    @Transactional
    void testCreateChapter_Success() {
        CreateChapterDto dto = new CreateChapterDto();
        dto.setChapterTitle("New Chapter");
        dto.setChapterText("New text content");

        ChapterDto created = chapterController.createChapter(testBook.getBookId(), dto);
        assertNotNull(created.getChapterId());
        assertEquals(1, created.getChapterNumber());
        assertEquals("New Chapter", created.getChapterTitle());
    }

    @Test
    void testCreateChapter_BookNotFound() {
        CreateChapterDto dto = new CreateChapterDto();
        dto.setChapterTitle("New");
        dto.setChapterText("Text");

        assertThrows(BookNotFoundException.class, () -> chapterController.createChapter(999L, dto));
    }

    @Test
    @Transactional
    void testUpdateChapter_NullFields_NotChanged() {
        Chapter c = createAndSaveChapter(testBook, 1, "Original", "OrigText");

        ChapterDto dto = new ChapterDto();
        dto.setChapterTitle("NewTitle");
        dto.setChapterText("NewText");
        dto.setChapterNumber(1);

        ChapterDto res = chapterController.updateChapter(c.getChapterId(), dto);
        assertEquals("NewTitle", res.getChapterTitle());
        assertEquals("NewText", res.getChapterText());
    }

    @Test
    @Transactional
    void testGetChapters_EmptyList() {
        List<ChapterDto> chapters = chapterController.getChapters(testBook.getBookId());
        assertTrue(chapters.isEmpty());
    }

    @Test
    @Transactional
    void testUpdateChapter_ChapterNotFound() {
        ChapterDto dto = new ChapterDto();
        dto.setChapterTitle("Upd");
        dto.setChapterText("UT");
        dto.setChapterNumber(1);

        assertThrows(ResponseStatusException.class, () -> chapterController.updateChapter(999L, dto));
    }

    @Test
    @Transactional
    void testGetChapter_Success() {
        Chapter c = createAndSaveChapter(testBook, 1, "Solo", "SoloText");
        chapterRepository.flush();

        ChapterDto res = chapterController.getChapter(c.getChapterId());
        assertEquals("Solo", res.getChapterTitle());
        assertEquals("SoloText", res.getChapterText());
    }

    @Test
    void testGetChapter_NotFound() {
        assertThrows(ResponseStatusException.class, () -> chapterController.getChapter(999L));
    }

    // ===== Вспомогательные методы =====

    private Chapter createAndSaveChapter(Book book, int number, String title, String text) {
        Chapter c = new Chapter();
        c.setChapterNumber(number);
        c.setChapterTitle(title);
        c.setChapterText(text);
        c.setBook(book);

        if (book.getChapters() != null) {
            book.getChapters().add(c);
        }

        return chapterRepository.save(c);
    }
}