package com.litsite.lit.controller;

import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/books")
public class ChapterController {
    private final ChapterService chapterService;
    @GetMapping("/{bookId}/chapters")
    public List<ChapterDto> getChapters(@PathVariable int bookId) {
        return chapterService.getChapters(bookId);
    }

    @PostMapping("/{bookId}/chapters")
    public ChapterDto createChapter(@PathVariable int bookId, @RequestBody CreateChapterDto chapterDto) {
        return chapterService.createChapter(bookId, chapterDto);
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterDto getChapter(@PathVariable int chapterId) {
        return chapterService.getChapter(chapterId);
    }

    @PutMapping("/chapters/{chapterId}")
    public ChapterDto updateChapter(@PathVariable int chapterId, @RequestBody ChapterDto chapterDto) {
        return chapterService.updateChapter(chapterId, chapterDto);
    }

    @DeleteMapping("/chapters/{chapterId}")
    public void deleteChapter(@PathVariable int chapterId) {
        chapterService.deleteChapter(chapterId);
    }
}