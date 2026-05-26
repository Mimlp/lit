package com.litsite.lit.dto;

import lombok.Data;

@Data
public class RatingDto {
    private Long ratingId;
    private AuthorDto author;
    private Integer rating;
}
