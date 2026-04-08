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
    public List<ChapterDto> getChapters(int bookId) {
        Book book = bookRepository.findWithChapters(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with id " + bookId + " not found"));
        // Убедимся, что главы отсортированы перед конвертацией
        List<Chapter> chapters = book.getChapters();
        chapters.sort((c1, c2) -> c1.getChapterNumber().compareTo(c2.getChapterNumber()));
        return bookMapper.chapterToChapterDtoList(chapters);
    }

    @Transactional
    public ChapterDto createChapter(int bookId, CreateChapterDto chapterDto) {
        Book book = bookRepository.findWithChapters(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with id " + bookId + " not found"));

        Chapter chapter = bookMapper.createChapterDtotoChapter(chapterDto);

        // Логика добавления в конец
        int chapterNumber = book.getChapters().size() + 1;
        chapter.setChapterNumber(chapterNumber);
        chapter.setBook(book);

        // Важно: добавляем в коллекцию книги и сохраняем книгу (каскад)
        book.getChapters().add(chapter);
        bookRepository.save(book);

        return bookMapper.chapterToChapterDto(chapter);
    }

    @Transactional(readOnly = true)
    public ChapterDto getChapter(int chapterId) {
        return bookMapper.chapterToChapterDto(
                chapterRepository.findById(chapterId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Chapter not found"
                        )));
    }

    @Transactional
    public ChapterDto updateChapter(int chapterId, ChapterDto chapterDto) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chapter not found"
                ));

        Integer oldNumber = chapter.getChapterNumber();
        Integer newNumber = chapterDto.getChapterNumber();
        boolean numberChanged = (newNumber != null && !newNumber.equals(oldNumber));

        // Обновляем текст всегда
        chapter.setChapterText(chapterDto.getChapterText());
        chapter.setChapterTitle(chapterDto.getChapterTitle());

        if (numberChanged) {
            // Получаем ВСЕ главы этой книги, отсортированные по номеру
            List<Chapter> allBookChapters = chapterRepository.findByBookBookIdOrderByChapterNumberAsc(chapter.getBook().getBookId());

            if (newNumber > oldNumber) {
                // Сдвигаем ВНИЗ (номер увеличивается): главы между old и new уменьшаем на 1
                // Пример: было 3, стало 5. Главы 4 и 5 становятся 3 и 4.
                for (Chapter c : allBookChapters) {
                    if (c.getChapterNumber() > oldNumber && c.getChapterNumber() <= newNumber) {
                        c.setChapterNumber(c.getChapterNumber() - 1);
                        chapterRepository.save(c);
                    }
                }
            } else if (newNumber < oldNumber) {
                // Сдвигаем ВВЕРХ (номер уменьшается): главы между new и old увеличиваем на 1
                // Пример: было 5, стало 3. Главы 3 и 4 становятся 4 и 5.
                for (Chapter c : allBookChapters) {
                    if (c.getChapterNumber() >= newNumber && c.getChapterNumber() < oldNumber) {
                        c.setChapterNumber(c.getChapterNumber() + 1);
                        chapterRepository.save(c);
                    }
                }
            }
            // Устанавливаем новый номер самой главе
            chapter.setChapterNumber(newNumber);
        }

        chapterRepository.save(chapter);
        return bookMapper.chapterToChapterDto(chapter);
    }

    @Transactional
    public void deleteChapter(int chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chapter not found"
                ));

        int chapterNumber = chapter.getChapterNumber();
        int bookId = chapter.getBook().getBookId();

        // Сначала удаляем главу
        chapterRepository.delete(chapter);

        // Сдвигаем номера всех последующих глав
        List<Chapter> chapters = chapterRepository.findByBookBookIdOrderByChapterNumberAsc(bookId);

        // Используем saveAll для оптимизации (один батч вместо N запросов)
        for (Chapter c : chapters) {
            if (c.getChapterNumber() > chapterNumber) {
                c.setChapterNumber(c.getChapterNumber() - 1);
            }
        }
        chapterRepository.saveAll(chapters);
    }
}