package com.litsite.lit.mapper;

import com.litsite.lit.dto.CommentDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Comment;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
<<<<<<< Updated upstream
    date = "2026-05-02T20:42:59+0400",
=======
    date = "2026-06-01T01:40:41+0400",
>>>>>>> Stashed changes
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class CommentMapperImpl implements CommentMapper {

    @Autowired
    private UserMapper userMapper;

    @Override
    public CommentDto commentToCommentDto(Comment comment, Long currentUserId) {
        if ( comment == null ) {
            return null;
        }

        CommentDto commentDto = new CommentDto();

        commentDto.setAuthor( userMapper.toAuthor( comment.getUser() ) );
        commentDto.setBookId( commentBookBookId( comment ) );
        commentDto.setCommentId( comment.getCommentId() );
        commentDto.setCommentText( comment.getCommentText() );
        commentDto.setCommentDate( comment.getCommentDate() );

        applyAuthorFlag( comment, commentDto, currentUserId );

        return commentDto;
    }

    @Override
    public List<CommentDto> commentsToCommentDtos(List<Comment> comments, Long currentUserId) {
        if ( comments == null ) {
            return null;
        }

        List<CommentDto> list = new ArrayList<CommentDto>( comments.size() );
        for ( Comment comment : comments ) {
            list.add( commentToCommentDto( comment, currentUserId ) );
        }

        return list;
    }

    private Long commentBookBookId(Comment comment) {
        Book book = comment.getBook();
        if ( book == null ) {
            return null;
        }
        return book.getBookId();
    }
}
