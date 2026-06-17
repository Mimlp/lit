package com.litsite.lit.chaptertest;

import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.exception.BookNotFoundException;
import com.litsite.lit.mapper.BookMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Chapter;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Role;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.ChapterRepository;
import com.litsite.lit.service.ChapterService;
import com.litsite.lit.service.EmailService;
import com.litsite.lit.controller.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookMapper bookMapper;
    @Mock private ChapterRepository chapterRepository;
    // ✅ ДОБАВЛЕНО: моки для зависимостей
    @Mock private AuthHelper authHelper;
    @Mock private EmailService emailService;

    @InjectMocks
    private ChapterService chapterService;

    private MyUser testUser;
    private MyUser otherUser;
    private Book testBook;
    private Chapter testChapter1;
    private Chapter testChapter2;
    private ChapterDto testChapterDto;
    private CreateChapterDto testCreateChapterDto;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setUsername("author");
        testUser.setEmail("author@example.com");

        otherUser = new MyUser();
        otherUser.setUserId(2L);
        otherUser.setUsername("other");

        testBook = new Book();
        testBook.setBookId(1L);
        testBook.setTitle("Test Book");
        testBook.setUser(testUser);
        testBook.setChapters(new ArrayList<>());

        testChapter1 = new Chapter();
        testChapter1.setChapterId(10L);
        testChapter1.setChapterNumber(1);
        testChapter1.setChapterTitle("Chapter One");
        testChapter1.setChapterText("Text one");
        testChapter1.setBook(testBook);

        testChapter2 = new Chapter();
        testChapter2.setChapterId(11L);
        testChapter2.setChapterNumber(2);
        testChapter2.setChapterTitle("Chapter Two");
        testChapter2.setChapterText("Text two");
        testChapter2.setBook(testBook);

        testBook.getChapters().add(testChapter1);
        testBook.getChapters().add(testChapter2);

        testChapterDto = new ChapterDto();
        testChapterDto.setChapterId(10L);
        testChapterDto.setChapterNumber(1);
        testChapterDto.setChapterTitle("Chapter One");
        testChapterDto.setChapterText("Text one");

        testCreateChapterDto = new CreateChapterDto();
        testCreateChapterDto.setChapterTitle("New Chapter");
        testCreateChapterDto.setChapterText("New text");

        // ✅ Базовые стабы для authHelper (только если нужны в конкретном тесте)
        // Не добавляйте в setUp(), если не все тесты используют authHelper!
    }

    @Test
    void testGetChapters_BookNotFound() {
        when(bookRepository.findWithChapters(999L)).thenReturn(Optional.empty());

        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> chapterService.getChapters(999L)
        );
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testGetChapters_Success() {
        when(bookRepository.findWithChapters(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.chapterToChapterDtoList(anyList()))
                .thenReturn(List.of(testChapterDto, testChapterDto));

        List<ChapterDto> result = chapterService.getChapters(1L);

        assertEquals(2, result.size());
        verify(bookMapper).chapterToChapterDtoList(argThat(list -> list.size() == 2));
    }

    @Test
    void testCreateChapter_BookNotFound() {
        when(bookRepository.findWithChapters(999L)).thenReturn(Optional.empty());

        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> chapterService.createChapter(999L, testCreateChapterDto)
        );
        assertTrue(exception.getMessage().contains("not found"));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void testGetChapter_Success() {
        when(chapterRepository.findByIdWithBookAndUser(10L)).thenReturn(Optional.of(testChapter1));
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        ChapterDto result = chapterService.getChapter(10L);

        assertNotNull(result);
        assertEquals(10L, result.getChapterId());
        verify(chapterRepository).findByIdWithBookAndUser(10L);
    }

    @Test
    void testGetChapter_NotFound() {
        when(chapterRepository.findByIdWithBookAndUser(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chapterService.getChapter(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testUpdateChapter_Success_NoNumberChange() {
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterTitle("Updated Title");
        updateDto.setChapterText("Updated Text");
        updateDto.setChapterNumber(1);

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(anyLong(), anyString(), anyString())).thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L)).thenReturn(Optional.of(testChapter1));
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter1);
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        assertNotNull(result);
        assertEquals("Updated Title", testChapter1.getChapterTitle());
        verify(chapterRepository).save(testChapter1);
    }

    @Test
    void testUpdateChapter_Success_NumberDecreased() {
        testChapter1.setChapterNumber(3);
        testChapter2.setChapterNumber(2);

        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterNumber(1);

        List<Chapter> allChapters = new ArrayList<>(List.of(testChapter2, testChapter1));

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(anyLong(), anyString(), anyString())).thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L)).thenReturn(Optional.of(testChapter1));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(1L)).thenReturn(allChapters);
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        assertNotNull(result);
        assertEquals(1, testChapter1.getChapterNumber());
        assertEquals(3, testChapter2.getChapterNumber());
    }

    @Test
    void testUpdateChapter_NotFound() {
        when(chapterRepository.findByIdWithBookAndUser(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chapterService.updateChapter(999L, new ChapterDto())
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void testDeleteChapter_NotFound() {
        when(chapterRepository.findByIdWithBookAndUser(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chapterService.deleteChapter(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(chapterRepository, never()).delete(any());
    }

    @Test
    void testGetChapters_SortedByNumber() {
        testChapter1.setChapterNumber(2);
        testChapter2.setChapterNumber(1);

        when(bookRepository.findWithChapters(1L)).thenReturn(Optional.of(testBook));

        when(bookMapper.chapterToChapterDtoList(anyList()))
                .thenAnswer(inv -> {
                    List<Chapter> list = inv.getArgument(0);
                    assertEquals(1, list.get(0).getChapterNumber());
                    assertEquals(2, list.get(1).getChapterNumber());
                    return List.of(testChapterDto);
                });

        chapterService.getChapters(1L);
    }

    @Test
    void testUpdateChapter_Success_NumberIncreased() {
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterNumber(3);

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(anyLong(), anyString(), anyString())).thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L)).thenReturn(Optional.of(testChapter1));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(1L))
                .thenReturn(new ArrayList<>(List.of(testChapter1, testChapter2)));
        when(chapterRepository.save(any(Chapter.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        assertNotNull(result);
        assertEquals(3, testChapter1.getChapterNumber());
        assertEquals(1, testChapter2.getChapterNumber());
        verify(chapterRepository, atLeastOnce()).save(any());
    }

    @Test
    void testDeleteChapter_Success() {
        // Arrange
        Long chapterId = 10L;
        Long bookOwnerId = 1L;  // testUser.getUserId()

        // 🔧 1. Мок текущего пользователя
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);

        // 🔧 2. Мок поиска главы
        when(chapterRepository.findByIdWithBookAndUser(chapterId))
                .thenReturn(Optional.of(testChapter1));

        // 🔧 3. Мок проверки прав — КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ!
        when(authHelper.isOwnerOrHasRole(eq(bookOwnerId), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);

        // 🔧 4. Мок получения оставшихся глав для перенумерации
        List<Chapter> remaining = new ArrayList<>(List.of(testChapter2));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(testBook.getBookId()))
                .thenReturn(remaining);

        // Act
        chapterService.deleteChapter(chapterId);  // или deleteChapter(chapterId, null)

        // Assert
        verify(authHelper).getCurrentUserOrThrow();
        verify(authHelper).isOwnerOrHasRole(eq(bookOwnerId), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN"));
        verify(chapterRepository).findByIdWithBookAndUser(chapterId);
        verify(chapterRepository).delete(testChapter1);
        verify(chapterRepository).saveAll(remaining);

        // Проверка, что номера глав пересчитались
        assertEquals(1, testChapter2.getChapterNumber());
    }

    // ==================== CREATE CHAPTER TESTS ====================

    @Test
    void testCreateChapter_Success_EmptyBook() {
        // Arrange
        testBook.getChapters().clear(); // Книга без глав
        CreateChapterDto dto = new CreateChapterDto();
        dto.setChapterTitle("First Chapter");
        dto.setChapterText("First text");

        Chapter newChapter = new Chapter();
        newChapter.setChapterId(99L);
        newChapter.setChapterNumber(1);
        newChapter.setChapterTitle("First Chapter");
        newChapter.setChapterText("First text");
        newChapter.setBook(testBook);

        when(bookRepository.findWithChapters(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.createChapterDtotoChapter(dto)).thenReturn(newChapter);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(newChapter);
        when(bookMapper.chapterToChapterDto(newChapter)).thenReturn(testChapterDto);

        // Act
        ChapterDto result = chapterService.createChapter(1L, dto);

        // Assert
        assertNotNull(result);
        assertEquals(1, newChapter.getChapterNumber());
        verify(chapterRepository).save(newChapter);
        verify(bookMapper).createChapterDtotoChapter(dto);
    }

    @Test
    void testCreateChapter_Success_WithExistingChapters() {
        // Arrange - в книге уже есть 2 главы
        CreateChapterDto dto = new CreateChapterDto();
        dto.setChapterTitle("Third Chapter");
        dto.setChapterText("Third text");

        Chapter newChapter = new Chapter();
        newChapter.setChapterId(99L);
        newChapter.setChapterNumber(3); // Должен быть 3-м
        newChapter.setChapterTitle("Third Chapter");
        newChapter.setBook(testBook);

        when(bookRepository.findWithChapters(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.createChapterDtotoChapter(dto)).thenReturn(newChapter);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(newChapter);
        when(bookMapper.chapterToChapterDto(newChapter)).thenReturn(testChapterDto);

        // Act
        ChapterDto result = chapterService.createChapter(1L, dto);

        // Assert
        assertNotNull(result);
        assertEquals(3, newChapter.getChapterNumber());
        verify(chapterRepository).save(newChapter);
    }

    // ==================== UPDATE CHAPTER - AUTHORIZATION TESTS ====================

    @Test
    void testUpdateChapter_Forbidden_NotOwnerAndNoRole() {
        // Arrange
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterTitle("Hacked");

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser); // Другой пользователь
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(false); // Нет прав
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chapterService.updateChapter(10L, updateDto)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void testUpdateChapter_Success_ModeratorCanUpdate() {
        // Arrange
        otherUser.addRole(new Role("ROLE_MODERATOR")); // Модератор
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterTitle("Moderated Title");
        updateDto.setChapterNumber(1); // Без изменения номера

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter1);
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        // Act
        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Moderated Title", testChapter1.getChapterTitle());
        verify(chapterRepository).save(testChapter1);
    }

    // ==================== DELETE CHAPTER - AUTHORIZATION & EMAIL TESTS ====================

    @Test
    void testDeleteChapter_Forbidden_NotOwnerAndNoRole() {
        // Arrange
        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(false);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chapterService.deleteChapter(10L)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(chapterRepository, never()).delete(any());
        verify(emailService, never()).sendContentModerationNotice(any(), any(), any(), any());
    }

    @Test
    void testDeleteChapter_ModeratorAction_SendsEmail() throws Exception {
        // Arrange
        String moderationReason = "Нарушение правил";
        testChapter1.setChapterTitle("Deleted Chapter");
        testBook.getUser().setEmail("author@example.com");

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser); // Модератор удаляет
        otherUser.addRole(new Role("ROLE_MODERATOR"));

        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(1L))
                .thenReturn(new ArrayList<>()); // Нет других глав для перенумерации

        // Act
        chapterService.deleteChapter(10L, moderationReason);

        // Assert
        verify(emailService).sendContentModerationNotice(
                eq("author@example.com"),
                eq("глава"),
                eq("Deleted Chapter"),
                eq(moderationReason)
        );
        verify(chapterRepository).delete(testChapter1);
    }

    @Test
    void testDeleteChapter_EmailException_IsHandled() {
        // Arrange
        testBook.getUser().setEmail("author@example.com");
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendContentModerationNotice(any(), any(), any(), any());

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        otherUser.addRole(new Role("ROLE_MODERATOR"));
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(1L))
                .thenReturn(new ArrayList<>());

        // Act & Assert - исключение в email не должно прерывать удаление
        assertDoesNotThrow(() -> chapterService.deleteChapter(10L, "reason"));

        verify(chapterRepository).delete(testChapter1);
        verify(emailService).sendContentModerationNotice(any(), any(), any(), any());
    }

    @Test
    void testDeleteChapter_OwnerAction_NoEmailSent() {
        // Arrange - владелец удаляет свою главу
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser); // Тот же владелец
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(1L))
                .thenReturn(new ArrayList<>());

        // Act
        chapterService.deleteChapter(10L);

        // Assert - email не отправляется, если удаляет владелец
        verify(emailService, never()).sendContentModerationNotice(any(), any(), any(), any());
        verify(chapterRepository).delete(testChapter1);
    }

    // ==================== GET CHAPTERS - EDGE CASES ====================

    @Test
    void testGetChapters_EmptyList_Sorted() {
        // Arrange - книга без глав
        testBook.getChapters().clear();
        when(bookRepository.findWithChapters(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.chapterToChapterDtoList(anyList())).thenReturn(List.of());

        // Act
        List<ChapterDto> result = chapterService.getChapters(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookMapper).chapterToChapterDtoList(argThat(List::isEmpty));
    }
    // ==================== UPDATE CHAPTER - EDGE CASES ====================

    @Test
    void testUpdateChapter_NumberUnchanged_OnlyTextUpdated() {
        // Arrange
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterText("New text only");
        updateDto.setChapterTitle("New title only");
        updateDto.setChapterNumber(1); // Тот же номер

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(anyLong(), anyString(), anyString())).thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter1);
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        // Act
        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        // Assert
        assertEquals("New text only", testChapter1.getChapterText());
        assertEquals("New title only", testChapter1.getChapterTitle());
        assertEquals(1, testChapter1.getChapterNumber()); // Номер не изменился
        // Перенумерация других глав не должна происходить
        verify(chapterRepository, times(1)).save(any(Chapter.class)); // Только одна глава
    }

    @Test
    void testUpdateChapter_NumberChangedToFirstPosition() {
        // Arrange - перемещаем главу #2 на позицию #1
        testChapter1.setChapterNumber(2);
        testChapter2.setChapterNumber(1);
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterNumber(1);

        List<Chapter> allChapters = new ArrayList<>(List.of(testChapter2, testChapter1));

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(anyLong(), anyString(), anyString())).thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.findByBookBookIdOrderByChapterNumberAsc(1L))
                .thenReturn(allChapters);
        when(chapterRepository.save(any(Chapter.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        // Act
        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        // Assert
        assertEquals(1, testChapter1.getChapterNumber()); // Перемещённая глава
        assertEquals(2, testChapter2.getChapterNumber()); // Сдвинутая глава
        verify(chapterRepository, times(2)).save(any(Chapter.class)); // Обе главы сохранены
    }

    @Test
    void testUpdateChapter_NullChapterNumber_IgnoresNumberChange() {
        // Arrange - DTO с null chapterNumber
        ChapterDto updateDto = new ChapterDto();
        updateDto.setChapterTitle("Updated");
        // chapterNumber не установлен = null

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(anyLong(), anyString(), anyString())).thenReturn(true);
        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter1);
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(testChapterDto);

        // Act
        ChapterDto result = chapterService.updateChapter(10L, updateDto);

        // Assert
        assertEquals("Updated", testChapter1.getChapterTitle());
        assertEquals(1, testChapter1.getChapterNumber()); // Номер не изменился
        verify(chapterRepository, times(1)).save(any(Chapter.class));
    }

    // ==================== GET CHAPTER - ADDITIONAL TESTS ====================

    @Test
    void testGetChapter_ReturnsMappedDto() {
        // Arrange
        ChapterDto expectedDto = new ChapterDto();
        expectedDto.setChapterId(10L);
        expectedDto.setChapterTitle("Chapter One");
        expectedDto.setChapterText("Text one");
        expectedDto.setChapterNumber(1);

        when(chapterRepository.findByIdWithBookAndUser(10L))
                .thenReturn(Optional.of(testChapter1));
        when(bookMapper.chapterToChapterDto(testChapter1)).thenReturn(expectedDto);

        // Act
        ChapterDto result = chapterService.getChapter(10L);

        // Assert
        assertEquals(expectedDto, result);
        verify(bookMapper).chapterToChapterDto(testChapter1);
    }
}