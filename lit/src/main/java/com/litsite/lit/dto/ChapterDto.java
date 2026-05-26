package com.litsite.lit.dto;

import lombok.Data;

@Data
public class ChapterDto {
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterText;
    private String chapterTitle;
}
