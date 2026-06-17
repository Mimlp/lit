<<<<<<< Updated upstream
// com.litsite.lit.service.RatingService.java
=======
>>>>>>> Stashed changes
package com.litsite.lit.service;

import com.litsite.lit.dto.CreateRatingRequest;
import com.litsite.lit.dto.RatingStats;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Rating;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final BookRepository bookRepository;

    public void rateBook(Long bookId, CreateRatingRequest request, MyUser user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Рейтинг должен быть от 1 до 5");
        }

        Rating existing = ratingRepository.findByBookBookIdAndUserUserId(bookId, user.getUserId())
                .orElse(null);

        if (existing != null) {
            existing.setRating(request.getRating());
            ratingRepository.save(existing);
        } else {
            Rating rating = new Rating();
            rating.setRating(request.getRating());
            rating.setBook(book);
            rating.setUser(user);
            ratingRepository.save(rating);
        }
    }

    public void deleteRating(Long bookId, MyUser user) {
        Rating rating = ratingRepository.findByBookBookIdAndUserUserId(bookId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рейтинг не найден"));

        ratingRepository.delete(rating);
    }

    public RatingStats getRatingStats(Long bookId) {
        Double average = ratingRepository.findAverageRatingByBookId(bookId).orElse(0.0);
        Long count = ratingRepository.countByBookBookId(bookId);
        return new RatingStats(average, count.intValue());
    }
}