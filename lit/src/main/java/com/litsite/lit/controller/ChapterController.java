package com.litsite.lit.controller;

import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/books")
public class ChapterController {
    private final ChapterService chapterService;
    private final AuthHelper authHelper;

    @GetMapping("/{bookId}/chapters")
    public List<ChapterDto> getChapters(@PathVariable long bookId) {
        return chapterService.getChapters(bookId);
    }

    @PostMapping("/{bookId}/chapters")
    public ChapterDto createChapter(@PathVariable long bookId, @RequestBody CreateChapterDto chapterDto) {
        return chapterService.createChapter(bookId, chapterDto);
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterDto getChapter(@PathVariable long chapterId) {
        return chapterService.getChapter(chapterId);
    }

    @PutMapping("/chapters/{chapterId}")
    public ChapterDto updateChapter(@PathVariable long chapterId, @RequestBody ChapterDto chapterDto) {
        return chapterService.updateChapter(chapterId, chapterDto);
    }

    @DeleteMapping("/chapters/{chapterId}")
<<<<<<< Updated upstream
    public void deleteChapter(@PathVariable long chapterId) {
        chapterService.deleteChapter(chapterId);
=======
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(
            @PathVariable long chapterId,
            @RequestParam(required = false) String reason) {

        chapterService.deleteChapter(chapterId, reason);
>>>>>>> Stashed changes
    }
}