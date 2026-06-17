package com.litsite.lit.repository;

import com.litsite.lit.models.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< Updated upstream

import java.util.List;
=======
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
>>>>>>> Stashed changes

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByBookBookIdOrderByCommentDateDesc(Long bookId);
<<<<<<< Updated upstream

=======
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Comment> findByIdWithUser(@Param("id") Long id);
>>>>>>> Stashed changes
    Page<Comment> findByBookBookId(Long bookId, Pageable pageable);
}
