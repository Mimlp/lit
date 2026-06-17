package com.litsite.lit.booktest;

import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.BookFilterRequest;
import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.dto.RatingStats;
import com.litsite.lit.dto.TagDto;
import com.litsite.lit.mapper.BookMapper;
import com.litsite.lit.mapper.TagMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Tag;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.TagRepository;
import com.litsite.lit.service.BookService;
import com.litsite.lit.service.EmailService;
import com.litsite.lit.service.RatingService;
import com.litsite.lit.controller.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private RatingService ratingService;
    @Mock
    private AuthHelper authHelper;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private BookService bookService;

    private Book testBook;
    private BookDto testBookDto;
    private BookSimpleDto testSimpleDto;
    private MyUser testUser;
    private Tag testTag;
    private TagDto testTagDto;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");

        testTag = new Tag();
        testTag.setTagId(1L);
        testTag.setTagName("Fantasy");

        testTagDto = new TagDto();
        testTagDto.setTagId(1L);
        testTagDto.setTagName("Fantasy");

        testBook = new Book();
        testBook.setBookId(1L);
        testBook.setTitle("Test Book");
        testBook.setDescription("Test Description");
        testBook.setUser(testUser);
        testBook.setTags(new HashSet<>(Set.of(testTag)));

        testBookDto = new BookDto();
        testBookDto.setBookId(1L);
        testBookDto.setTitle("Test Book");
        testBookDto.setDescription("Test Description");
        testBookDto.setRating(4.5);
        testBookDto.setRatingCount(10);

        testSimpleDto = new BookSimpleDto();
        testSimpleDto.setBookId(1L);
        testSimpleDto.setTitle("Test Book");
        testSimpleDto.setRating(4.5);
        testSimpleDto.setRatingCount(10);
    }

    // ==================== Базовые тесты ====================

    @Test
    void testGetBookById_Success() {
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.bookToBookDto(testBook, 1L)).thenReturn(testBookDto);
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));
        when(authHelper.getCurrentUserId()).thenReturn(1L);

        BookDto result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getBookId());
        assertEquals("Test Book", result.getTitle());
        assertEquals(4.5, result.getRating());
        verify(bookRepository).findByIdWithUser(1L);
        verify(bookMapper).bookToBookDto(testBook, 1L);
    }

    @Test
    void testGetBookById_NotFound() {
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.getBookById(1L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testGetAllBooks() {
        List<Book> books = List.of(testBook);
        when(bookRepository.findAll()).thenReturn(books);
        when(bookMapper.booksToBookSimpleDtos(books)).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.getAllBooks();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
        verify(bookRepository).findAll();
    }

    @Test
    void testGetMyBooks() {
        when(authHelper.getCurrentUserId()).thenReturn(1L);
        when(bookRepository.findByUserUserIdOrderByPublicationDateDesc(1L))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.getMyBooks();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(authHelper).getCurrentUserId();
        verify(bookRepository).findByUserUserIdOrderByPublicationDateDesc(1L);
    }

    @Test
    void testGetBooksByUserId() {
        when(bookRepository.findByUserUserIdOrderByPublicationDateDesc(2L))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.getBooksByUserId(2L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookRepository).findByUserUserIdOrderByPublicationDateDesc(2L);
    }

    @Test
    void testGetMyBooks_Unauthenticated() {
        when(authHelper.getCurrentUserId()).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.getMyBooks()
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(bookRepository, never()).findByUserUserIdOrderByPublicationDateDesc(anyLong());
    }

    // ==================== Тесты обновления/удаления ====================

    @Test
    void testUpdateBook() {
        // Arrange
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Updated Title");
        updateDto.setDescription("Updated Description");

        // 🔧 1. Исправляем мок репозитория: findById → findByIdWithUser
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testBook));

        // 🔧 2. Мокаем проверку прав доступа (используется в сервисе)
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);

        // 🔧 3. Мокаем save и mapper с правильными аргументами
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        when(bookMapper.bookToBookDto(any(Book.class), eq(1L))).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.updateBook(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Title", testBook.getTitle());  // проверяем, что книга обновилась
        assertEquals("Updated Description", testBook.getDescription());

        // Verify
        verify(bookRepository).findByIdWithUser(1L);
        verify(authHelper).getCurrentUserOrThrow();
        verify(authHelper).isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN"));
        verify(bookRepository).save(any(Book.class));
        verify(bookMapper).bookToBookDto(any(Book.class), eq(1L));
    }

    @Test
    void testUpdateBook_NotFound() {
        // 🔧 Исправляем: findById → findByIdWithUser
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.empty());
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);

        BookDto updateDto = new BookDto();
        updateDto.setTitle("New");
        updateDto.setDescription("Desc");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.updateBook(1L, updateDto)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(bookRepository).findByIdWithUser(1L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testDeleteBook() {
        // Arrange
        Long bookId = 1L;

        // 🔧 1. Мок текущего пользователя
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);

        // 🔧 2. Мок поиска книги (findByIdWithUser, а не findById!)
        when(bookRepository.findByIdWithUser(bookId)).thenReturn(Optional.of(testBook));

        // 🔧 3. Мок проверки прав (владелец или модератор/админ)
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);

        // Act
        bookService.deleteBook(bookId);  // или deleteBook(bookId, null)

        // Assert
        verify(authHelper).getCurrentUserOrThrow();
        verify(bookRepository).findByIdWithUser(bookId);
        verify(authHelper).isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN"));
        verify(bookRepository).delete(testBook);  // 🔧 delete(entity), а не deleteById(id)!
    }

    @Test
    void testUpdateBookTags_Success() {
        Long bookId = 1L;
        Set<Long> tagIds = Set.of(2L);
        Tag newTag = new Tag();
        newTag.setTagId(2L);
        newTag.setTagName("Sci-Fi");

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        when(tagRepository.findAllById(tagIds)).thenReturn(List.of(newTag));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookMapper.bookToBookDto(any(Book.class), eq(testUser.getUserId()))).thenReturn(testBookDto);

        BookDto result = bookService.updateBookTags(bookId, tagIds, testUser);

        assertNotNull(result);
        verify(tagRepository).findAllById(tagIds);
        verify(bookRepository).save(any(Book.class));
        verify(bookMapper).bookToBookDto(any(Book.class), eq(testUser.getUserId()));
        assertEquals(1, testBook.getTags().size());
        assertTrue(testBook.getTags().contains(newTag));
    }

    @Test
    void testUpdateBookTags_BookNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.updateBookTags(1L, Set.of(1L), testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testUpdateBookTags_Forbidden() {
        MyUser otherUser = new MyUser();
        otherUser.setUserId(99L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.updateBookTags(1L, Set.of(1L), otherUser)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookRepository, never()).save(any());
    }

    // ==================== Тесты тегов ====================

    @Test
    void testGetAllTags() {
        List<Tag> tags = List.of(testTag);
        when(tagRepository.findAll()).thenReturn(tags);
        when(tagMapper.tagToTagDtoList(tags)).thenReturn(List.of(testTagDto));

        List<TagDto> result = bookService.getAllTags();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fantasy", result.get(0).getTagName());
        verify(tagRepository).findAll();
    }

    @Test
    void testAddNewTag_Success() {
        when(tagRepository.findByTagNameIgnoreCase("Fantasy")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);
        when(tagMapper.tagToTagDto(testTag)).thenReturn(testTagDto);

        TagDto result = bookService.addNewTag(testTagDto);

        assertNotNull(result);
        assertEquals("Fantasy", result.getTagName());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void testAddNewTag_AlreadyExists() {
        when(tagRepository.findByTagNameIgnoreCase("Fantasy")).thenReturn(Optional.of(testTag));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.addNewTag(testTagDto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // ==================== Тесты поиска с новой логикой ====================

    @Test
    void testFindBooksByFilter_WithKeywordAndIncludeTags() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword("magic");
        filter.setIncludeTagIds(Set.of(1L, 2L));
        filter.setExcludeTagIds(null);

        when(bookRepository.findByKeywordAndTags("magic", Set.of(1L, 2L), 2L, null))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookRepository).findByKeywordAndTags("magic", Set.of(1L, 2L), 2L, null);
        verify(ratingService).getRatingStats(1L);
    }

    @Test
    void testFindBooksByFilter_WithExcludeTags() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword(null);
        filter.setIncludeTagIds(null);
        filter.setExcludeTagIds(Set.of(3L));

        when(bookRepository.findByKeywordAndTags(null, null, 0, Set.of(3L)))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        verify(bookRepository).findByKeywordAndTags(null, null, 0, Set.of(3L));
    }

    @Test
    void testFindBooksByFilter_WithIncludeAndExcludeTags() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword("test");
        filter.setIncludeTagIds(Set.of(1L));
        filter.setExcludeTagIds(Set.of(5L));

        when(bookRepository.findByKeywordAndTags("test", Set.of(1L), 1L, Set.of(5L)))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        verify(bookRepository).findByKeywordAndTags("test", Set.of(1L), 1L, Set.of(5L));
    }

    @Test
    void testFindBooksByFilter_BlankKeywordAndEmptyIncludeTags_TreatedAsNull() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword("   ");
        filter.setIncludeTagIds(Set.of()); // пустой набор → null
        filter.setExcludeTagIds(null);

        when(bookRepository.findByKeywordAndTags(null, null, 0, null))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Проверяем, что пустые коллекции были преобразованы в null
        verify(bookRepository).findByKeywordAndTags(null, null, 0, null);
    }

    @Test
    void testFindBooksByFilter_OnlyKeyword() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword("test");
        filter.setIncludeTagIds(null);
        filter.setExcludeTagIds(null);

        when(bookRepository.findByKeywordAndTags("test", null, 0, null))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        verify(bookRepository).findByKeywordAndTags("test", null, 0, null);
    }

    @Test
    void testFindBooksByFilter_OnlyIncludeTags() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword(null);
        filter.setIncludeTagIds(Set.of(1L, 2L));
        filter.setExcludeTagIds(null);

        when(bookRepository.findByKeywordAndTags(null, Set.of(1L, 2L), 2L, null))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        verify(bookRepository).findByKeywordAndTags(null, Set.of(1L, 2L), 2L, null);
    }

    @Test
    void testFindBooksByFilter_OnlyExcludeTags() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword(null);
        filter.setIncludeTagIds(null);
        filter.setExcludeTagIds(Set.of(3L, 4L));

        when(bookRepository.findByKeywordAndTags(null, null, 0, Set.of(3L, 4L)))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        verify(bookRepository).findByKeywordAndTags(null, null, 0, Set.of(3L, 4L));
    }

    @Test
    void testFindBooksByFilter_AllNull_ReturnsAll() {
        BookFilterRequest filter = new BookFilterRequest();
        filter.setKeyword(null);
        filter.setIncludeTagIds(null);
        filter.setExcludeTagIds(null);

        when(bookRepository.findByKeywordAndTags(null, null, 0, null))
                .thenReturn(List.of(testBook));
        when(bookMapper.booksToBookSimpleDtos(anyList())).thenReturn(List.of(testSimpleDto));
        when(ratingService.getRatingStats(1L)).thenReturn(new RatingStats(4.5, 10));

        List<BookSimpleDto> result = bookService.findBooksByFilter(filter);

        assertNotNull(result);
        verify(bookRepository).findByKeywordAndTags(null, null, 0, null);
    }
    // ==================== Тесты удаления с email уведомлением ====================

    @Test
    void testDeleteBook_ByModerator_SendsEmail() {
        // Arrange
        MyUser moderator = new MyUser();
        moderator.setUserId(99L);

        // 🔧 Убедитесь, что у владельца книги есть email
        testBook.getUser().setEmail("test@example.com");

        when(authHelper.getCurrentUserOrThrow()).thenReturn(moderator);
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testBook));
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);
        doNothing().when(emailService).sendContentModerationNotice(anyString(), anyString(), anyString(), anyString());

        // Act
        bookService.deleteBook(1L, "Нарушение правил");

        // Assert
        verify(emailService).sendContentModerationNotice(
                eq("test@example.com"),
                eq("книга"),
                eq("Test Book"),
                eq("Нарушение правил")
        );
        verify(bookRepository).delete(testBook);
    }

    @Test
    void testDeleteBook_Forbidden() {
        // Arrange
        MyUser otherUser = new MyUser();
        otherUser.setUserId(99L);

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testBook));
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.deleteBook(1L)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookRepository, never()).delete(any());
    }

// ==================== Тесты updateBook с проверкой прав ====================

    @Test
    void testUpdateBook_Forbidden() {
        // Arrange
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Updated");

        MyUser otherUser = new MyUser();
        otherUser.setUserId(99L);

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testBook));
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookService.updateBook(1L, updateDto)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testUpdateBook_ByModerator_Success() {
        // Arrange
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Updated by mod");

        MyUser moderator = new MyUser();
        moderator.setUserId(99L);

        when(authHelper.getCurrentUserOrThrow()).thenReturn(moderator);
        when(bookRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testBook));
        when(authHelper.isOwnerOrHasRole(eq(1L), eq("ROLE_MODERATOR"), eq("ROLE_ADMIN")))
                .thenReturn(true);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        when(bookMapper.bookToBookDto(any(Book.class), eq(99L))).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.updateBook(1L, updateDto);

        // Assert
        assertNotNull(result);
        verify(bookRepository).save(testBook);
    }
}