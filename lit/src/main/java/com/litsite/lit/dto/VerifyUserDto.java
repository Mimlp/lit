package com.litsite.lit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyUserDto {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 6)
    private String verificationCode;
}
