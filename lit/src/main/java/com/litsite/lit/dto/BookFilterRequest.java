package com.litsite.lit.dto;

import lombok.Data;
import java.util.Set;

@Data
public class BookFilterRequest {
    private String keyword;
<<<<<<< Updated upstream
    private Set<Long> tagIds;
=======
    private Set<Long> includeTagIds;
    private Set<Long> excludeTagIds;
>>>>>>> Stashed changes
}