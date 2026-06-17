package com.litsite.lit.repository;

import com.litsite.lit.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
<<<<<<< Updated upstream

public interface BookRepository extends JpaRepository<Book, Long> {
=======

public interface BookRepository extends JpaRepository<Book, Long> {

>>>>>>> Stashed changes
    @Query("""
            select distinct b from Book b
            left join fetch b.chapters
            where b.bookId = :id
            """)
    Optional<Book> findWithChapters(@Param("id") Long id);
<<<<<<< Updated upstream
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
=======

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.user WHERE b.bookId = :id")
    Optional<Book> findByIdWithUser(@Param("id") Long id);

    List<Book> findByUserUserIdOrderByPublicationDateDesc(Long userId);

    @Query("""
    SELECT DISTINCT b FROM Book b
    LEFT JOIN b.tags t
    WHERE
      (:keyword IS NULL OR LOWER(b.title) LIKE CONCAT('%', :keyword, '%'))
      AND (:includeTagIds IS NULL OR t.tagId IN :includeTagIds)
      AND (:excludeTagIds IS NULL OR t.tagId NOT IN :excludeTagIds)
    GROUP BY b.bookId
    HAVING :includeTagIds IS NULL 
           OR COUNT(DISTINCT CASE WHEN t.tagId IN :includeTagIds THEN t.tagId END) = :includeTagIdsSize
    """)
    List<Book> findByKeywordAndTags(
            @Param("keyword") String keyword,  // Уже в lowercase из Java
            @Param("includeTagIds") Set<Long> includeTagIds,
            @Param("includeTagIdsSize") long includeTagIdsSize,
            @Param("excludeTagIds") Set<Long> excludeTagIds
    );
}
>>>>>>> Stashed changes
