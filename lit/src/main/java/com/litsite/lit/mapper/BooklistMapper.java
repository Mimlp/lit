package com.litsite.lit.mapper;

import com.litsite.lit.dto.BooklistDto;
import com.litsite.lit.models.BookList;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

<<<<<<< Updated upstream
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {BookMapper.class})
=======
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {BookMapper.class, UserMapper.class})
>>>>>>> Stashed changes
public interface BooklistMapper {
    BookList bookListDtoToBookList(BooklistDto bookListDto);
    BooklistDto bookListToBooklistDto(BookList bookList);
    List<BookList> bookListDtoToBookList(List<BooklistDto> bookListDto);
    List<BooklistDto> bookListToBooklistDto(List<BookList> bookLists);
}
