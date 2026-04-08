package com.litsite.lit.repository;

import com.litsite.lit.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Integer> {
    @Query("""
            select distinct b from Book b
                               left join fetch b.chapters
                                      where b.bookId = :id
            """)
    Optional<Book> findWithChapters(@Param("id") Integer id);
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.user WHERE b.bookId = :id")
    Optional<Book> findByIdWithUser(@Param("id") Integer id);
    List<Book> findByUserUserIdOrderByPublicationDateDesc(Integer userId);
}
