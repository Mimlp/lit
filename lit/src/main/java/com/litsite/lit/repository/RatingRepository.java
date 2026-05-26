package com.litsite.lit.repository;

import com.litsite.lit.models.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByBookBookIdAndUserUserId(Long bookId, Long userId);

    @Query("SELECT ROUND(AVG(r.rating), 1) FROM Rating r WHERE r.book.bookId = :bookId")
    Optional<Double> findAverageRatingByBookId(@Param("bookId") Long bookId);

    long countByBookBookId(Long bookId);

    void deleteByBookBookId(Long bookId);
}