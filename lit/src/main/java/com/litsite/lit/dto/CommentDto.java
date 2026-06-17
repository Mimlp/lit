<<<<<<< Updated upstream
// com.litsite.lit.dto.CommentDto
=======
>>>>>>> Stashed changes
package com.litsite.lit.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDto {
    private Long commentId;
    private String commentText;
    private LocalDateTime commentDate;
    private AuthorDto author;
    private Long bookId;
    private boolean currentUserIsAuthor;
}