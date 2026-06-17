package com.litsite.lit.commenttest;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.CommentDto;
import com.litsite.lit.mapper.CommentMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Comment;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.CommentRepository;
import com.litsite.lit.service.CommentService;
import com.litsite.lit.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private AuthHelper authHelper;
    @Mock private EmailService emailService;

    @InjectMocks
    private CommentService commentService;

    private MyUser testUser;
    private MyUser otherUser;
    private Book testBook;
    private Comment testComment;
    private CommentDto testCommentDto;
    private AuthorDto testAuthorDto;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setUsername("commenter");
        testUser.setEmail("commenter@test.com");

        otherUser = new MyUser();
        otherUser.setUserId(2L);
        otherUser.setUsername("other");

        testBook = new Book();
        testBook.setBookId(10L);
        testBook.setTitle("Test Book");

        testComment = new Comment();
        testComment.setCommentId(100L);
        testComment.setCommentText("Great book!");
        testComment.setCommentDate(LocalDateTime.now());
        testComment.setBook(testBook);
        testComment.setUser(testUser);

        testCommentDto = new CommentDto();
        testCommentDto.setCommentId(100L);
        testCommentDto.setCommentText("Great book!");
        testCommentDto.setCommentDate(LocalDateTime.now());
        testCommentDto.setBookId(10L);
        testCommentDto.setCurrentUserIsAuthor(true);

        testAuthorDto = new AuthorDto();
        testAuthorDto.setUserId(1L);
        testAuthorDto.setUsername("commenter");
    }

    @Test
    void testCreateComment_Success() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.commentToCommentDto(eq(testComment), eq(1L))).thenReturn(testCommentDto);

        CommentDto result = commentService.createComment(testCommentDto, 10L);

        assertNotNull(result);
        assertEquals(100L, result.getCommentId());
        assertEquals("Great book!", result.getCommentText());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void testFindByBookId_Success() {
        when(authHelper.getCurrentUserId()).thenReturn(1L);
        when(commentRepository.findByBookBookIdOrderByCommentDateDesc(10L))
                .thenReturn(List.of(testComment));
        when(commentMapper.commentsToCommentDtos(List.of(testComment), 1L))
                .thenReturn(List.of(testCommentDto));

        List<CommentDto> result = commentService.findByBookId(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Great book!", result.get(0).getCommentText());
        verify(commentRepository).findByBookBookIdOrderByCommentDateDesc(10L);
    }

    @Test
    void testFindByBookId_EmptyList() {
        when(authHelper.getCurrentUserId()).thenReturn(1L);
        when(commentRepository.findByBookBookIdOrderByCommentDateDesc(10L))
                .thenReturn(List.of());
        when(commentMapper.commentsToCommentDtos(List.of(), 1L))
                .thenReturn(List.of());

        List<CommentDto> result = commentService.findByBookId(10L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testChangeComment_Success() {
        CommentDto updateDto = new CommentDto();
        updateDto.setCommentText("Updated comment");

        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), anyString(), anyString())).thenReturn(true);
        when(commentRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.commentToCommentDto(eq(testComment), eq(1L))).thenReturn(testCommentDto);

        CommentDto result = commentService.changeComment(updateDto, 100L);

        assertNotNull(result);
        assertEquals("Updated comment", testComment.getCommentText());
        verify(commentRepository).save(testComment);
    }

    @Test
    void testChangeComment_NotFound() {
        // 🔧 Не мокаем authHelper — он не вызывается, если комментарий не найден
        when(commentRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> commentService.changeComment(new CommentDto(), 999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void testChangeComment_Forbidden() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), anyString(), anyString())).thenReturn(false);
        when(commentRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testComment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> commentService.changeComment(new CommentDto(), 100L)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void testDeleteComment_Success() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), anyString(), anyString())).thenReturn(true);
        when(commentRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testComment));

        assertDoesNotThrow(() -> commentService.deleteComment(100L));

        verify(commentRepository).delete(testComment);
    }

    @Test
    void testDeleteComment_NotFound() {
        // 🔧 Не мокаем authHelper — он не вызывается
        when(commentRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> commentService.deleteComment(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void testDeleteComment_Forbidden() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.isOwnerOrHasRole(eq(1L), anyString(), anyString())).thenReturn(false);
        when(commentRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testComment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> commentService.deleteComment(100L)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void testFindByBookId_SortedByDateDesc() {
        Comment olderComment = new Comment();
        olderComment.setCommentId(99L);
        olderComment.setCommentText("Older");
        olderComment.setCommentDate(LocalDateTime.now().minusDays(1));
        olderComment.setBook(testBook);
        olderComment.setUser(testUser);

        when(authHelper.getCurrentUserId()).thenReturn(1L);
        when(commentRepository.findByBookBookIdOrderByCommentDateDesc(10L))
                .thenReturn(List.of(testComment, olderComment));
        when(commentMapper.commentsToCommentDtos(anyList(), eq(1L)))
                .thenReturn(List.of(testCommentDto));

        List<CommentDto> result = commentService.findByBookId(10L);
        assertNotNull(result);
        verify(commentRepository).findByBookBookIdOrderByCommentDateDesc(10L);
    }

    @Test
    void testCreateComment_BookNotFound() {
        // 🔧 Книга не найдена — проверяется ПЕРЕД авторизацией
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.createComment(testCommentDto, 999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        // 🔧 Убрали лишний мок: authHelper.getCurrentUserOrThrow()
    }

    @Test
    void testDeleteComment_Unauthenticated() {
        // 🔧 1. Мокаем поиск — чтобы код дошёл до проверки авторизации
        when(commentRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testComment));

        // 🔧 2. Теперь мокаем авторизацию — она будет вызвана
        when(authHelper.getCurrentUserOrThrow()).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.deleteComment(100L)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void testChangeComment_Unauthenticated() {
        // 🔧 1. Мокаем поиск комментария
        when(commentRepository.findByIdWithUser(100L)).thenReturn(Optional.of(testComment));

        // 🔧 2. Мокаем авторизацию
        when(authHelper.getCurrentUserOrThrow()).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.changeComment(new CommentDto(), 100L)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void testCreateComment_Unauthenticated() {
        // 🔧 1. Мокаем поиск книги — чтобы код дошёл до авторизации
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));

        // 🔧 2. Мокаем авторизацию
        when(authHelper.getCurrentUserOrThrow()).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.createComment(testCommentDto, 10L)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}