package com.litsite.lit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRightsDto {
    private boolean isAuthor;
    private boolean isAuthenticated;
}
