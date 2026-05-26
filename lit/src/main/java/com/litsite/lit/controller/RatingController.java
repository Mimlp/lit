// com.litsite.lit.controller.RatingController.java
package com.litsite.lit.controller;

import com.litsite.lit.dto.CreateRatingRequest;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;
    private final AuthHelper authHelper;

    @PutMapping("/{bookId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rateBook(
            @PathVariable Long bookId,
            @RequestBody CreateRatingRequest request
    ) {
        MyUser user = authHelper.getCurrentUserOrThrow();
        ratingService.rateBook(bookId, request, user);
    }

    @DeleteMapping("/{bookId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable Long bookId) {
        MyUser user = authHelper.getCurrentUserOrThrow();
        ratingService.deleteRating(bookId, user);
    }
}