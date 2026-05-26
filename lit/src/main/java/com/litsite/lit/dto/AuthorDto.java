package com.litsite.lit.dto;

import lombok.Data;

@Data
public class AuthorDto {
    private Long userId;
    private String username;
    private String profileDescription;
}
