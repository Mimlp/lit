package com.litsite.lit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDto{
    private Long userId;
    private String username;
    private String email;
    private String profileDescription;
    private LocalDateTime registrationDate;
    private Boolean isEnabled;
    private List<String> roles;
    private String avatarUrl;
}
