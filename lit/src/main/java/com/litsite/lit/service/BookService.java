package com.litsite.lit.service;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.dto.*;
import com.litsite.lit.mapper.BookMapper;
import com.litsite.lit.mapper.TagMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Tag;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.TagRepository;
import com.litsite.lit.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final TagMapper tagMapper;
    private final TagRepository tagRepository;
    private final RatingService ratingService;
    private final AuthHelper authHelper;

    @Transactional
    public BookDto getBookById(Long id) {
        Book book = bookRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        BookDto dto = bookMapper.bookToBookDto(book, authHelper.getCurrentUserId());
        enrichWithRating(dto, id);
        return dto;
    }

    public List<BookSimpleDto> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        return enrichSimpleDtosWithRating(books);
    }

    public List<BookSimpleDto> getMyBooks() {
        Long userId = authHelper.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        List<Book> books = bookRepository.findByUserUserIdOrderByPublicationDateDesc(userId);
        return enrichSimpleDtosWithRating(books);
    }

    public List<BookSimpleDto> getBooksByUserId(Long userId) {
        List<Book> books = bookRepository.findByUserUserIdOrderByPublicationDateDesc(userId);
        return enrichSimpleDtosWithRating(books);
    }

    private List<BookSimpleDto> enrichSimpleDtosWithRating(List<Book> books) {
        List<BookSimpleDto> dtos = bookMapper.booksToBookSimpleDtos(books);

        Map<Long, RatingStats> statsMap = books.stream()
                .collect(Collectors.toMap(
                        Book::getBookId,
                        book -> ratingService.getRatingStats(book.getBookId())
                ));

        for (int i = 0; i < dtos.size(); i++) {
            RatingStats stats = statsMap.get(books.get(i).getBookId());
            dtos.get(i).setRating(stats.getAverage());
            dtos.get(i).setRatingCount(stats.getCount());
        }

        return dtos;
    }

    @Transactional
    public BookDto updateBookTags(Long bookId, Set<Long> tagIds, MyUser currentUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

        if (!book.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Только автор может изменять теги книги");
        }

        Set<Tag> tags = tagRepository.findAllById(tagIds)
                .stream()
                .collect(Collectors.toSet());

        book.getTags().clear();
        book.getTags().addAll(tags);

        for (Tag tag : tags) {
            tag.getBooks().add(book);
        }

        Book saved = bookRepository.save(book);
        return bookMapper.bookToBookDto(saved, currentUser.getUserId());
    }

    private void enrichWithRating(BookDto dto, Long bookId) {
        var stats = ratingService.getRatingStats(bookId);
        dto.setRating(stats.getAverage());
        dto.setRatingCount(stats.getCount());
    }

    public BookDto updateBook(Long id, BookDto bookDto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        book.setTitle(bookDto.getTitle());
        book.setDescription(bookDto.getDescription());
        return bookMapper.bookToBookDto(bookRepository.save(book), getCurrentUserId());
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.user().getUserId();
        }
        return null;
    }

    public List<BookSimpleDto> findBooksByFilter(BookFilterRequest filter) {
        String keyword = filter.getKeyword();
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        Set<Long> tagIds = filter.getTagIds();
        if (tagIds != null && tagIds.isEmpty()) {
            tagIds = null;
        }

        return enrichSimpleDtosWithRating(
                bookRepository.findByKeywordAndTags(keyword, tagIds)
        );
    }

    public List<TagDto> getAllTags() {
        return tagMapper.tagToTagDtoList(tagRepository.findAll());
    }

    public TagDto addNewTag(TagDto tagDto) {
        if (tagRepository.findByTagNameIgnoreCase(tagDto.getTagName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag already exists");
        }
        Tag tag = new Tag();
        tag.setTagName(tagDto.getTagName());
        tag = tagRepository.save(tag);
        return tagMapper.tagToTagDto(tag);
    }
}