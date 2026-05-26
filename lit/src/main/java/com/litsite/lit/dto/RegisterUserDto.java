package com.litsite.lit.dto;

import lombok.Data;

@Data
public class RegisterUserDto {
    private String email;
    private String password;
    private String login;
    private String username;
}
