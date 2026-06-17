package com.litsite.lit.ratingtest;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.controller.RatingController;
import com.litsite.lit.dto.CreateRatingRequest;
import com.litsite.lit.dto.RatingStats;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.RatingRepository;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.RatingService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class RatingControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public AuthHelper authHelper() { return mock(AuthHelper.class); }
    }

    @Autowired private RatingController ratingController;
    @Autowired private RatingService ratingService;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthHelper authHelper;

    private MyUser testUser;
    private Book testBook;

    @BeforeEach void before() {
        testUser = new MyUser(); testUser.setUsername("r"); testUser.setEmail("r@t.com");
        testUser.setPasswordHash("p"); testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        lenient().when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        lenient().when(authHelper.getCurrentUserId()).thenReturn(testUser.getUserId());

        testBook = new Book(); testBook.setTitle("RB"); testBook.setPublicationDate(LocalDateTime.now());
        testBook = bookRepository.save(testBook);
    }

    @AfterEach void after() {
        ratingRepository.deleteAllInBatch(); bookRepository.deleteAllInBatch(); userRepository.deleteAllInBatch();
    }

    @Test @Transactional void testRateBook_NewRating_Success() {
        CreateRatingRequest req = new CreateRatingRequest(); req.setRating(5);
        ratingController.rateBook(testBook.getBookId(), req);
        assertEquals(5.0, ratingService.getRatingStats(testBook.getBookId()).getAverage());
    }

    @Test @Transactional void testRateBook_InvalidRating_TooLow() {
        CreateRatingRequest req = new CreateRatingRequest(); req.setRating(0);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ratingController.rateBook(testBook.getBookId(), req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test @Transactional void testDeleteRating_Success() {
        CreateRatingRequest req = new CreateRatingRequest(); req.setRating(4);
        ratingController.rateBook(testBook.getBookId(), req);
        ratingController.deleteRating(testBook.getBookId());
        assertEquals(0, ratingService.getRatingStats(testBook.getBookId()).getCount());
    }

    @Test void testDeleteRating_NotFound() {
        assertThrows(ResponseStatusException.class, () -> ratingController.deleteRating(testBook.getBookId()));
    }
}