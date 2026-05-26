package com.litsite.lit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
@Data
public class BookSimpleDto {
    private Long bookId;
    private String title;
    private String description;
    private LocalDateTime publicationDate;
    private Integer viewsAmount;
    private AuthorDto author;
    private Double rating;
    private Integer ratingCount;
    private Set<TagDto> tags;
}
