package com.litsite.lit.mapper;

import com.litsite.lit.dto.CommentDto;
import com.litsite.lit.models.Comment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface CommentMapper {

    @Mapping(source = "user", target = "author")
    @Mapping(source = "book.bookId", target = "bookId")
    CommentDto commentToCommentDto(Comment comment, @Context Long currentUserId);

    @AfterMapping
    default void applyAuthorFlag(Comment source, @MappingTarget CommentDto target, @Context Long currentUserId) {
        boolean isAuthor = currentUserId != null
                && source.getUser() != null
                && source.getUser().getUserId().equals(currentUserId);
        target.setCurrentUserIsAuthor(isAuthor);
    }

    List<CommentDto> commentsToCommentDtos(List<Comment> comments, @Context Long currentUserId);
}