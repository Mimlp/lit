package com.litsite.lit.ratingtest;

import com.litsite.lit.dto.CreateRatingRequest;
import com.litsite.lit.dto.RatingStats;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Rating;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.RatingRepository;
import com.litsite.lit.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private RatingService ratingService;

    private MyUser testUser;
    private Book testBook;
    private CreateRatingRequest testRequest;
    private Rating existingRating;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");

        testBook = new Book();
        testBook.setBookId(10L);
        testBook.setTitle("Test Book");

        testRequest = new CreateRatingRequest();
        testRequest.setRating(4);

        existingRating = new Rating();
        existingRating.setRatingId(100L);
        existingRating.setRating(3);
        existingRating.setBook(testBook);
        existingRating.setUser(testUser);
    }

    @Test
    void testRateBook_NewRating_Success() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(ratingRepository.findByBookBookIdAndUserUserId(10L, 1L)).thenReturn(Optional.empty());

        ratingService.rateBook(10L, testRequest, testUser);

        verify(ratingRepository).save(any(Rating.class));
        verify(ratingRepository, times(0)).save(existingRating);
    }

    @Test
    void testRateBook_UpdateExistingRating_Success() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(ratingRepository.findByBookBookIdAndUserUserId(10L, 1L)).thenReturn(Optional.of(existingRating));

        ratingService.rateBook(10L, testRequest, testUser);

        assertEquals(4, existingRating.getRating());
        verify(ratingRepository).save(existingRating);
    }

    @Test
    void testRateBook_BookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.rateBook(999L, testRequest, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void testRateBook_RatingTooLow() {
        testRequest.setRating(0);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.rateBook(10L, testRequest, testUser)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testRateBook_RatingTooHigh() {
        testRequest.setRating(6);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.rateBook(10L, testRequest, testUser)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testDeleteRating_Success() {
        when(ratingRepository.findByBookBookIdAndUserUserId(10L, 1L)).thenReturn(Optional.of(existingRating));

        ratingService.deleteRating(10L, testUser);

        verify(ratingRepository).delete(existingRating);
    }

    @Test
    void testDeleteRating_NotFound() {
        when(ratingRepository.findByBookBookIdAndUserUserId(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.deleteRating(10L, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    void testGetRatingStats_Success() {
        when(ratingRepository.findAverageRatingByBookId(10L)).thenReturn(Optional.of(4.5));
        when(ratingRepository.countByBookBookId(10L)).thenReturn(2L);

        RatingStats stats = ratingService.getRatingStats(10L);

        assertNotNull(stats);
        assertEquals(4.5, stats.getAverage());
        assertEquals(2, stats.getCount());
    }

    @Test
    void testGetRatingStats_NoRatings() {
        when(ratingRepository.findAverageRatingByBookId(10L)).thenReturn(Optional.empty());
        when(ratingRepository.countByBookBookId(10L)).thenReturn(0L);

        RatingStats stats = ratingService.getRatingStats(10L);

        assertEquals(0.0, stats.getAverage());
        assertEquals(0, stats.getCount());
    }
}