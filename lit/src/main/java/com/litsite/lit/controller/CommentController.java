package com.litsite.lit.controller;

import com.litsite.lit.dto.CommentDto;
<<<<<<< Updated upstream
=======
import com.litsite.lit.models.MyUser;
>>>>>>> Stashed changes
import com.litsite.lit.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
<<<<<<< Updated upstream
=======
    private final AuthHelper authHelper;
>>>>>>> Stashed changes

    @PostMapping("/{bookId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable Long bookId,
            @RequestBody CommentDto commentDto
    ) {
        return commentService.createComment(commentDto, bookId);
    }

    @GetMapping("/{bookId}/comments")
    public List<CommentDto> getBookComments(@PathVariable Long bookId) {
        return commentService.findByBookId(bookId);
    }

    @PutMapping("/comments/{commentId}")
    public CommentDto updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentDto commentDto
    ) {
        return commentService.changeComment(commentDto, commentId);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
<<<<<<< Updated upstream
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
=======
    public void deleteComment(
            @PathVariable Long commentId,
            @RequestParam(required = false) String reason) {

        commentService.deleteComment(commentId, reason);
>>>>>>> Stashed changes
    }
}