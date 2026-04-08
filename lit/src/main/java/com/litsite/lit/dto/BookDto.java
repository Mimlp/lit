package com.litsite.lit.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class BookDto {
    private Integer bookId;
    private String title;
    private String description;
    private LocalDateTime publicationDate;
    private Integer viewsAmount;
    private List<ChapterDto> chapters;
    private AuthorDto author;
    private Set<TagDto> tags;
    private UserRightsDto userRights;
}