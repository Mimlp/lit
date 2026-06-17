package com.litsite.lit.service;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.dto.CommentDto;
import com.litsite.lit.mapper.CommentMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Comment;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.CommentRepository;
<<<<<<< Updated upstream
=======
import jakarta.mail.MessagingException;
>>>>>>> Stashed changes
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final BookRepository bookRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final AuthHelper authHelper;
<<<<<<< Updated upstream
=======
    private final EmailService emailService;
>>>>>>> Stashed changes

    public CommentDto createComment(CommentDto commentDto, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

        MyUser currentUser = authHelper.getCurrentUserOrThrow();

        Comment comment = new Comment();
        comment.setCommentText(commentDto.getCommentText());
        comment.setBook(book);
        comment.setUser(currentUser);
        comment.setCommentDate(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);

        return commentMapper.commentToCommentDto(saved, currentUser.getUserId());
    }

    public List<CommentDto> findByBookId(Long bookId) {
        Long currentUserId = authHelper.getCurrentUserId();
        return commentMapper.commentsToCommentDtos(
                commentRepository.findByBookBookIdOrderByCommentDateDesc(bookId),
                currentUserId
        );
    }

    public CommentDto changeComment(CommentDto commentDto, Long commentId) {
<<<<<<< Updated upstream
        Comment comment = commentRepository.findById(commentId)
=======
        Comment comment = commentRepository.findByIdWithUser(commentId)
>>>>>>> Stashed changes
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));

        MyUser currentUser = authHelper.getCurrentUserOrThrow();

<<<<<<< Updated upstream
        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно редактировать только свои комментарии");
=======
        if (!authHelper.isOwnerOrHasRole(comment.getUser().getUserId(), "ROLE_MODERATOR", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
>>>>>>> Stashed changes
        }

        comment.setCommentText(commentDto.getCommentText());
        return commentMapper.commentToCommentDto(commentRepository.save(comment), currentUser.getUserId());
    }

<<<<<<< Updated upstream
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
=======
    public void deleteComment(Long commentId, String moderationReason) {
        Comment comment = commentRepository.findByIdWithUser(commentId)
>>>>>>> Stashed changes
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));

        MyUser currentUser = authHelper.getCurrentUserOrThrow();

<<<<<<< Updated upstream
        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно удалять только свои комментарии");
=======
        if (!authHelper.isOwnerOrHasRole(comment.getUser().getUserId(), "ROLE_MODERATOR", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        boolean isModeratorAction = !currentUser.getUserId().equals(comment.getUser().getUserId());
        if (isModeratorAction && comment.getUser().getEmail() != null) {
            try {
                String preview = comment.getCommentText().length() > 100
                        ? comment.getCommentText().substring(0, 100) + "..."
                        : comment.getCommentText();
                emailService.sendContentModerationNotice(
                        comment.getUser().getEmail(),
                        "комментарий",
                        preview,
                        moderationReason != null ? moderationReason : "Нарушение правил сообщества"
                );
            } catch (Exception e) {
                System.err.println("Failed to send comment moderation email: " + e.getMessage());
            }
>>>>>>> Stashed changes
        }

        commentRepository.delete(comment);
    }
<<<<<<< Updated upstream
=======

    public void deleteComment(Long commentId) {
        deleteComment(commentId, null);
    }
>>>>>>> Stashed changes
}