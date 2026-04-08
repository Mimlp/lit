package com.litsite.lit.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserDto{
    private Integer userId;
    private String login;
    private String username;
    private String email;
    private String profileDescription;
    private LocalDateTime registrationDate;
    private Boolean isEnabled;
    private String roles;
}
