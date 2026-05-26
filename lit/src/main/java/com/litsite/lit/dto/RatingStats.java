// com.litsite.lit.dto.RatingStats.java (вспомогательный класс)
package com.litsite.lit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingStats {
    private Double average;
    private Integer count;
}