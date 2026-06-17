package com.litsite.lit.commenttest;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.controller.CommentController;
import com.litsite.lit.dto.CommentDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Comment;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.CommentRepository;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.CommentService;
import com.litsite.lit.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CommentControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public AuthHelper authHelper() { return mock(AuthHelper.class); }

        @Bean @Primary
        public EmailService emailService() { return mock(EmailService.class); }
    }

    @Autowired private CommentController commentController;
    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthHelper authHelper;  // Это МОК из TestConfig
    @Autowired private EmailService emailService;  // Это тоже МОК

    private MyUser testUser;
    private MyUser moderatorUser;
    private Book testBook;
    private Comment testComment;

    @BeforeEach
    void before() {
        // Создаём пользователей
        testUser = createUser("commenter", "commenter@test.com");
        moderatorUser = createUser("mod", "mod@test.com");

        // 🔧 КЛЮЧЕВОЙ ФИКС: мок основного метода авторизации
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);

        // Вспомогательные моки
        when(authHelper.getCurrentUserId()).thenReturn(testUser.getUserId());
        when(authHelper.isOwner(eq(testUser.getUserId()))).thenReturn(true);
        when(authHelper.isOwner(eq(moderatorUser.getUserId()))).thenReturn(false);
        when(authHelper.isAdminOrModerator()).thenReturn(false);
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(true);
        when(authHelper.isOwnerOrHasRole(eq(moderatorUser.getUserId()), any(), any())).thenReturn(false);
        doNothing().when(emailService).sendContentModerationNotice(anyString(), anyString(), anyString(), anyString());

        // Создаём книгу и комментарий
        testBook = createBook("BookForComments", testUser);
        testComment = createComment(testBook, testUser, "Great comment");
    }

    @AfterEach
    void after() {
        reset(authHelper, emailService);
        commentRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
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

    private Book createBook(String title, MyUser owner) {
        Book book = new Book();
        book.setTitle(title);
        book.setPublicationDate(LocalDateTime.now());
        book.setUser(owner);
        return bookRepository.save(book);
    }

    private Comment createComment(Book book, MyUser author, String text) {
        Comment comment = new Comment();
        comment.setCommentText(text);
        comment.setCommentDate(LocalDateTime.now());
        comment.setBook(book);
        comment.setUser(author);
        return commentRepository.save(comment);
    }

    // ===== Тесты =====

    @Test
    @Transactional
    void testGetBookComments_Success() {
        List<CommentDto> list = commentController.getBookComments(testBook.getBookId());
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    @Transactional
    void testCreateComment_Success() {
        // 🔧 Явно настраиваем мок для этого теста
        when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);

        CommentDto dto = new CommentDto();
        dto.setCommentText("New comment text");
        CommentDto created = commentController.createComment(testBook.getBookId(), dto);

        assertNotNull(created.getCommentId());
        assertEquals("New comment text", created.getCommentText());
    }

    @Test
    @Transactional
    void testDeleteComment_Success_Moderator() throws MessagingException {
        // 🔧 Переопределяем моки для сценария модератора
        when(authHelper.getCurrentUserOrThrow()).thenReturn(moderatorUser);
        when(authHelper.getCurrentUserId()).thenReturn(moderatorUser.getUserId());
        when(authHelper.isAdminOrModerator()).thenReturn(true);
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(true);

        String reason = "Оскорбительный контент";
        commentController.deleteComment(testComment.getCommentId(), reason);

        assertTrue(commentRepository.findById(testComment.getCommentId()).isEmpty());

        verify(emailService, times(1)).sendContentModerationNotice(
                eq("commenter@test.com"),
                eq("комментарий"),
                argThat(s -> s.contains("Great comment")),
                eq(reason)
        );
    }

    @Test @Transactional void testGetBookComments_BookNotFound() {
        List<CommentDto> list = commentController.getBookComments(999L);
        assertTrue(list.isEmpty());
    }

    @Test
    @Transactional
    void testUpdateComment_Success() {
        // 🔧 Не нужно дополнительно настраивать мок — он уже в @BeforeEach!

        CommentDto dto = new CommentDto();
        dto.setCommentText("Updated text");
        CommentDto updated = commentController.updateComment(testComment.getCommentId(), dto);
        assertEquals("Updated text", updated.getCommentText());
    }

    @Test @Transactional void testUpdateComment_Forbidden_OtherUser() {
        when(authHelper.getCurrentUserOrThrow()).thenReturn(moderatorUser);
        when(authHelper.getCurrentUserId()).thenReturn(moderatorUser.getUserId());
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(false);

        CommentDto dto = new CommentDto();
        dto.setCommentText("Hacked");

        assertThrows(ResponseStatusException.class, () ->
                commentController.updateComment(testComment.getCommentId(), dto));
    }

    @Test
    @Transactional
    void testDeleteComment_Success_Owner() {
        // 🔧 Тоже не нужно — мок уже установлен!

        commentController.deleteComment(testComment.getCommentId(), null);
        Optional<Comment> deleted = commentRepository.findById(testComment.getCommentId());
        assertTrue(deleted.isEmpty());
        verify(emailService, never()).sendContentModerationNotice(anyString(), anyString(), anyString(), anyString());
    }

    @Test @Transactional void testDeleteComment_Forbidden_OtherUser() {
        MyUser otherUser = new MyUser();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPasswordHash("hash");
        otherUser.setRegistrationDate(LocalDateTime.now());
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        when(authHelper.getCurrentUserOrThrow()).thenReturn(otherUser);
        when(authHelper.getCurrentUserId()).thenReturn(otherUser.getUserId());
        when(authHelper.isOwnerOrHasRole(eq(testUser.getUserId()), any(), any())).thenReturn(false);

        assertThrows(ResponseStatusException.class, () ->
                commentController.deleteComment(testComment.getCommentId(), null));
    }
}