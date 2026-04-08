package com.litsite.lit.mapper;

import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.dto.UserRightsDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Chapter;
import org.mapstruct.*;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class, TagMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookMapper {

    // Добавляем контекст: currentUserId (null для анонимного пользователя)
    @Mapping(source = "user", target = "author")
    @Mapping(source = "tags", target = "tags")
    BookDto bookToBookDto(Book book, @Context Integer currentUserId);

    // Маппер сам вызовет этот метод после основного преобразования
    @AfterMapping
    default void applyUserRights(Book source, @MappingTarget BookDto target, @Context Integer currentUserId) {
        boolean isAuthenticated = currentUserId != null;
        boolean isAuthor = isAuthenticated
                && source.getUser() != null
                && source.getUser().getUserId() == currentUserId;

        target.setUserRights(new UserRightsDto(isAuthor, isAuthenticated));
    }

    // Остальные методы без изменений (контекст им не нужен)
    Book bookDtoToBook(BookDto bookDto);
    ChapterDto chapterToChapterDto(Chapter chapter);
    Chapter chapterDtoToChapter(ChapterDto chapterDto);
    List<ChapterDto> chapterToChapterDtoList(List<Chapter> chapters);
    Chapter createChapterDtotoChapter(CreateChapterDto chapterDto);
}