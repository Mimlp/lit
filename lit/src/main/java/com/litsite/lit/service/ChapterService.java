package com.litsite.lit.service;

import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.exception.BookNotFoundException;
import com.litsite.lit.mapper.BookMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Chapter;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final ChapterRepository chapterRepository;

    @Transactional(readOnly = true)
    public List<ChapterDto> getChapters(Long bookId) {
        Book book = bookRepository.findWithChapters(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with id " + bookId + " not found"));
        List<Chapter> chapters = book.getChapters();
        chapters.sort((c1, c2) -> c1.getChapterNumber().compareTo(c2.getChapterNumber()));
        return bookMapper.chapterToChapterDtoList(chapters);
    }

    @Transactional
    public ChapterDto createChapter(Long bookId, CreateChapterDto chapterDto) {
        Book book = bookRepository.findWithChapters(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with id " + bookId + " not found"));

        Chapter chapter = bookMapper.createChapterDtotoChapter(chapterDto);
        int chapterNumber = book.getChapters().size() + 1;
        chapter.setChapterNumber(chapterNumber);
        chapter.setBook(book);
        chapterRepository.save(chapter);

        return bookMapper.chapterToChapterDto(chapter);
    }

    @Transactional(readOnly = true)
    public ChapterDto getChapter(Long chapterId) {
        return bookMapper.chapterToChapterDto(
                chapterRepository.findById(chapterId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Chapter not found"
                        )));
    }

    @Transactional
    public ChapterDto updateChapter(Long chapterId, ChapterDto chapterDto) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chapter not found"
                ));

        Integer oldNumber = chapter.getChapterNumber();
        Integer newNumber = chapterDto.getChapterNumber();
        boolean numberChanged = (newNumber != null && !newNumber.equals(oldNumber));

        chapter.setChapterText(chapterDto.getChapterText());
        chapter.setChapterTitle(chapterDto.getChapterTitle());

        if (numberChanged) {
            List<Chapter> allBookChapters = chapterRepository.findByBookBookIdOrderByChapterNumberAsc(chapter.getBook().getBookId());

            if (newNumber > oldNumber) {
                for (Chapter c : allBookChapters) {
                    if (c.getChapterNumber() > oldNumber && c.getChapterNumber() <= newNumber) {
                        c.setChapterNumber(c.getChapterNumber() - 1);
                        chapterRepository.save(c);
                    }
                }
            } else if (newNumber < oldNumber) {
                for (Chapter c : allBookChapters) {
                    if (c.getChapterNumber() >= newNumber && c.getChapterNumber() < oldNumber) {
                        c.setChapterNumber(c.getChapterNumber() + 1);
                        chapterRepository.save(c);
                    }
                }
            }
            chapter.setChapterNumber(newNumber);
        }

        chapterRepository.save(chapter);
        return bookMapper.chapterToChapterDto(chapter);
    }

    @Transactional
    public void deleteChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chapter not found"
                ));

        int chapterNumber = chapter.getChapterNumber();
        long bookId = chapter.getBook().getBookId();

        chapterRepository.delete(chapter);

        List<Chapter> chapters = chapterRepository.findByBookBookIdOrderByChapterNumberAsc(bookId);

        for (Chapter c : chapters) {
            if (c.getChapterNumber() > chapterNumber) {
                c.setChapterNumber(c.getChapterNumber() - 1);
            }
        }
        chapterRepository.saveAll(chapters);
    }
}