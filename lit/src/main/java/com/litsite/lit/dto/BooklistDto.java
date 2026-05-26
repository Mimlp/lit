package com.litsite.lit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class BooklistDto {
    private Long listId;
    private String title;
    private LocalDateTime creationDate;
    private Set<BookSimpleDto> books = new HashSet<>();
}
