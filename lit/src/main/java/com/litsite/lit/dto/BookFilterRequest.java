package com.litsite.lit.dto;

import lombok.Data;
import java.util.Set;

@Data
public class BookFilterRequest {
    private String keyword;
    private Set<Long> tagIds;
}