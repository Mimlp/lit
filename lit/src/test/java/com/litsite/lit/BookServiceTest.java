package com.litsite.lit;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.dto.BookDto;
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
import com.litsite.lit.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        testBook.setTags(Set.of(testTag));

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
    void testUpdateBook() {
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Updated Title");
        updateDto.setDescription("Updated Description");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        when(bookMapper.bookToBookDto(any(Book.class), any())).thenReturn(testBookDto);

        BookDto result = bookService.updateBook(1L, updateDto);

        assertNotNull(result);
        verify(bookRepository).findById(1L);
        verify(bookRepository).save(any(Book.class));
        verify(bookMapper).bookToBookDto(any(Book.class), isNull());
    }

    @Test
    void testDeleteBook() {
        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

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
}