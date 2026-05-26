package com.litsite.lit.repository;

import com.litsite.lit.models.BookList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BooklistRepository extends JpaRepository<BookList, Long> {
    Optional<BookList> findByTitle(String title);
    List<BookList> findByUserUserId(Long userId);
}
