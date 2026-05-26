package com.litsite.lit.mapper;

import com.litsite.lit.dto.*;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Chapter;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class, TagMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookMapper {

    @Mapping(source = "user", target = "author")
    @Mapping(source = "tags", target = "tags")
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    BookDto bookToBookDto(Book book, @Context Long currentUserId);

    @AfterMapping
    default void applyUserRights(Book source, @MappingTarget BookDto target, @Context Long currentUserId) {
        boolean isAuthenticated = currentUserId != null;
        boolean isAuthor = isAuthenticated
                && source.getUser() != null
                && source.getUser().getUserId().equals(currentUserId);
        target.setUserRights(new UserRightsDto(isAuthor, isAuthenticated));
    }

    @Mapping(source = "user", target = "author")
    @Mapping(source = "tags", target = "tags")
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    BookSimpleDto bookToBookSimpleDto(Book book);

    List<BookSimpleDto> booksToBookSimpleDtos(List<Book> books);

    Book bookDtoToBook(BookDto bookDto);

    ChapterDto chapterToChapterDto(Chapter chapter);

    Chapter chapterDtoToChapter(ChapterDto chapterDto);

    List<ChapterDto> chapterToChapterDtoList(List<Chapter> chapters);

    Chapter createChapterDtotoChapter(CreateChapterDto chapterDto);

    Set<BookSimpleDto> booksToBookSimpleDtos(Set<Book> books);
}