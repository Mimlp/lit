package com.litsite.lit.booklisttest;

import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.dto.BooklistDto;
import com.litsite.lit.mapper.BooklistMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.BookList;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.BooklistRepository;
import com.litsite.lit.service.BooklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooklistServiceTest {

    @Mock
    private BooklistRepository booklistRepository;
    @Mock
    private BooklistMapper booklistMapper;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BooklistService booklistService;

    private MyUser testUser;
    private MyUser otherUser;
    private BookList testBookList;
    private BooklistDto testBooklistDto;
    private Book testBook;
    private BookSimpleDto testBookSimpleDto;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");

        otherUser = new MyUser();
        otherUser.setUserId(2L);
        otherUser.setUsername("otheruser");

        testBook = new Book();
        testBook.setBookId(10L);
        testBook.setTitle("Test Book");

        testBookSimpleDto = new BookSimpleDto();
        testBookSimpleDto.setBookId(10L);
        testBookSimpleDto.setTitle("Test Book");

        testBookList = new BookList();
        testBookList.setListId(1L);
        testBookList.setTitle("My Reading List");
        testBookList.setCreationDate(LocalDateTime.now());
        testBookList.setUser(testUser);
        testBookList.setBooks(new HashSet<>(Set.of(testBook)));

        testBooklistDto = new BooklistDto();
        testBooklistDto.setListId(1L);
        testBooklistDto.setTitle("My Reading List");
        testBooklistDto.setCreationDate(LocalDateTime.now());
        testBooklistDto.setBooks(Set.of(testBookSimpleDto));
    }

    @Test
    void testFindByUserId_Success() {
        when(booklistRepository.findByUserUserId(1L)).thenReturn(List.of(testBookList));
        when(booklistMapper.bookListToBooklistDto(List.of(testBookList))).thenReturn(List.of(testBooklistDto));

        List<BooklistDto> result = booklistService.findByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("My Reading List", result.get(0).getTitle());
        verify(booklistRepository).findByUserUserId(1L);
        verify(booklistMapper).bookListToBooklistDto(List.of(testBookList));
    }

    @Test
    void testFindByUserId_EmptyResult() {
        when(booklistRepository.findByUserUserId(99L)).thenReturn(List.of());
        when(booklistMapper.bookListToBooklistDto(List.of())).thenReturn(List.of());

        List<BooklistDto> result = booklistService.findByUserId(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateBooklist_Success() {
        BookList newBookList = new BookList();
        newBookList.setListId(2L);
        newBookList.setTitle("New List");
        newBookList.setUser(testUser);

        BooklistDto newDto = new BooklistDto();
        newDto.setListId(2L);
        newDto.setTitle("New List");

        when(booklistRepository.save(any(BookList.class))).thenReturn(newBookList);
        when(booklistMapper.bookListToBooklistDto(newBookList)).thenReturn(newDto);

        BooklistDto result = booklistService.createBooklist("New List", testUser);

        assertNotNull(result);
        assertEquals(2L, result.getListId());
        assertEquals("New List", result.getTitle());
        verify(booklistRepository).save(any(BookList.class));
    }

    @Test
    void testAddBookToList_Success() {
        Book newBook = new Book();
        newBook.setBookId(20L);
        newBook.setTitle("Another Book");

        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(newBook));
        when(booklistRepository.save(any(BookList.class))).thenReturn(testBookList);
        when(booklistMapper.bookListToBooklistDto(testBookList)).thenReturn(testBooklistDto);

        BooklistDto result = booklistService.addBookToList(1L, 20L, testUser);

        assertNotNull(result);
        assertTrue(testBookList.getBooks().contains(newBook));
        verify(booklistRepository).save(testBookList);
    }

    @Test
    void testAddBookToList_BooklistNotFound() {
        when(booklistRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.addBookToList(999L, 1L, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(booklistRepository, never()).save(any());
    }

    @Test
    void testAddBookToList_BookNotFound() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.addBookToList(1L, 999L, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testAddBookToList_Forbidden() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.addBookToList(1L, 1L, otherUser)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(booklistRepository, never()).save(any());
    }

    @Test
    void testRemoveBookFromList_BooklistNotFound() {
        when(booklistRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.removeBookFromList(999L, 1L, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testRemoveBookFromList_BookNotFound() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.removeBookFromList(1L, 999L, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testRemoveBookFromList_Forbidden() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.removeBookFromList(1L, 1L, otherUser)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void testGetById_Success() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(booklistMapper.bookListToBooklistDto(testBookList)).thenReturn(testBooklistDto);

        BooklistDto result = booklistService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getListId());
        assertEquals("My Reading List", result.getTitle());
    }

    @Test
    void testGetById_NotFound() {
        when(booklistRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.getById(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testUpdateBooklist_Success() {
        BooklistDto updateDto = new BooklistDto();
        updateDto.setTitle("Updated Title");

        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(booklistRepository.save(any(BookList.class))).thenReturn(testBookList);
        when(booklistMapper.bookListToBooklistDto(testBookList)).thenReturn(testBooklistDto);

        BooklistDto result = booklistService.updateBooklist(1L, updateDto, testUser);

        assertNotNull(result);
        assertEquals("Updated Title", testBookList.getTitle());
        verify(booklistRepository).save(testBookList);
    }

    @Test
    void testUpdateBooklist_NullTitle_NotChanged() {
        BooklistDto updateDto = new BooklistDto();
        updateDto.setTitle(null);

        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(booklistRepository.save(any(BookList.class))).thenReturn(testBookList);
        when(booklistMapper.bookListToBooklistDto(testBookList)).thenReturn(testBooklistDto);

        booklistService.updateBooklist(1L, updateDto, testUser);

        assertEquals("My Reading List", testBookList.getTitle()); // Не изменилось
    }

    @Test
    void testUpdateBooklist_BlankTitle_NotChanged() {
        BooklistDto updateDto = new BooklistDto();
        updateDto.setTitle("   ");

        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(booklistRepository.save(any(BookList.class))).thenReturn(testBookList);
        when(booklistMapper.bookListToBooklistDto(testBookList)).thenReturn(testBooklistDto);

        booklistService.updateBooklist(1L, updateDto, testUser);

        assertEquals("My Reading List", testBookList.getTitle()); // Не изменилось
    }

    @Test
    void testUpdateBooklist_NotFound() {
        when(booklistRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.updateBooklist(999L, new BooklistDto(), testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(booklistRepository, never()).save(any());
    }

    @Test
    void testUpdateBooklist_Forbidden() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.updateBooklist(1L, new BooklistDto(), otherUser)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(booklistRepository, never()).save(any());
    }

    @Test
    void testDeleteBooklist_Success() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));

        booklistService.deleteBooklist(1L, testUser);

        verify(booklistRepository).delete(testBookList);
    }

    @Test
    void testDeleteBooklist_NotFound() {
        when(booklistRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.deleteBooklist(999L, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(booklistRepository, never()).delete(any());
    }

    @Test
    void testDeleteBooklist_Forbidden() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> booklistService.deleteBooklist(1L, otherUser)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(booklistRepository, never()).delete(any());
    }

    @Test
    void testAddBookToList_BookAlreadyInList() {
        when(booklistRepository.findById(1L)).thenReturn(Optional.of(testBookList));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(booklistMapper.bookListToBooklistDto(testBookList)).thenReturn(testBooklistDto);

        BooklistDto result = booklistService.addBookToList(1L, 10L, testUser);

        assertNotNull(result);
        verify(booklistRepository, never()).save(any());
    }
}