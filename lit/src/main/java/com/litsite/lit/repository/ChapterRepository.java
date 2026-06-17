package com.litsite.lit.repository;

import com.litsite.lit.models.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByBookBookIdOrderByChapterNumberAsc(Long bookId);
<<<<<<< Updated upstream
=======
    @Query("SELECT c FROM Chapter c JOIN FETCH c.book b JOIN FETCH b.user WHERE c.id = :id")
    Optional<Chapter> findByIdWithBookAndUser(@Param("id") Long id);
>>>>>>> Stashed changes
}
