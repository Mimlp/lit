package com.litsite.lit.repository;

import com.litsite.lit.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("""
            select distinct b from Book b
                               left join fetch b.chapters
                                      where b.bookId = :id
            """)
    Optional<Book> findWithChapters(@Param("id") Long id);
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.user WHERE b.bookId = :id")
    Optional<Book> findByIdWithUser(@Param("id") Long id);
    List<Book> findByUserUserIdOrderByPublicationDateDesc(Long userId);
    @Query("""
    SELECT DISTINCT b FROM Book b
    LEFT JOIN b.tags t
    WHERE 
        (:keyword IS NULL OR b.title ILIKE '%' || CAST(:keyword AS text) || '%')
        AND (:tagIds IS NULL OR t.tagId IN :tagIds)
    """)
    List<Book> findByKeywordAndTags(
            @Param("keyword") String keyword,
            @Param("tagIds") Set<Long> tagIds
    );
}
