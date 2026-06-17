<<<<<<< Updated upstream
// com.litsite.lit.controller.RatingController.java
package com.litsite.lit.controller;

import com.litsite.lit.dto.CreateRatingRequest;
=======
package com.litsite.lit.controller;

import com.litsite.lit.dto.CreateRatingRequest;
import com.litsite.lit.dto.RatingDto;
import com.litsite.lit.dto.RatingStats;
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    @PutMapping("/{bookId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
=======
    @PostMapping("/{bookId}/rating")
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
=======

    @GetMapping("/{bookId}/rating")
    public RatingStats getRating(@PathVariable Long bookId) {
        return ratingService.getRatingStats(bookId);
    }
>>>>>>> Stashed changes
}