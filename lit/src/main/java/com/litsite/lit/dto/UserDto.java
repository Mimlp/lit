package com.litsite.lit.dto;

import lombok.Data;

@Data
public class UserDto {
    String userId;
    String firstName;
    String lastName;
    String email;
    String passwordHash;
}
