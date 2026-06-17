package com.litsite.lit.mapper;

import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.dto.BooklistDto;
import com.litsite.lit.dto.TagDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.BookList;
import com.litsite.lit.models.Tag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
public class BooklistMapperImpl implements BooklistMapper {

    @Autowired
    private BookMapper bookMapper;

    @Override
    public BookList bookListDtoToBookList(BooklistDto bookListDto) {
        if ( bookListDto == null ) {
            return null;
        }

        BookList bookList = new BookList();

        bookList.setListId( bookListDto.getListId() );
        bookList.setTitle( bookListDto.getTitle() );
        bookList.setCreationDate( bookListDto.getCreationDate() );
        bookList.setBooks( bookSimpleDtoSetToBookSet( bookListDto.getBooks() ) );

        return bookList;
    }

    @Override
    public BooklistDto bookListToBooklistDto(BookList bookList) {
        if ( bookList == null ) {
            return null;
        }

        BooklistDto booklistDto = new BooklistDto();

        booklistDto.setListId( bookList.getListId() );
        booklistDto.setTitle( bookList.getTitle() );
        booklistDto.setCreationDate( bookList.getCreationDate() );
        booklistDto.setBooks( bookMapper.booksToBookSimpleDtos( bookList.getBooks() ) );

        return booklistDto;
    }

    @Override
    public List<BookList> bookListDtoToBookList(List<BooklistDto> bookListDto) {
        if ( bookListDto == null ) {
            return null;
        }

        List<BookList> list = new ArrayList<BookList>( bookListDto.size() );
        for ( BooklistDto booklistDto : bookListDto ) {
            list.add( bookListDtoToBookList( booklistDto ) );
        }

        return list;
    }

    @Override
    public List<BooklistDto> bookListToBooklistDto(List<BookList> bookLists) {
        if ( bookLists == null ) {
            return null;
        }

        List<BooklistDto> list = new ArrayList<BooklistDto>( bookLists.size() );
        for ( BookList bookList : bookLists ) {
            list.add( bookListToBooklistDto( bookList ) );
        }

        return list;
    }

    protected Tag tagDtoToTag(TagDto tagDto) {
        if ( tagDto == null ) {
            return null;
        }

        Tag tag = new Tag();

        tag.setTagId( tagDto.getTagId() );
        tag.setTagName( tagDto.getTagName() );

        return tag;
    }

    protected Set<Tag> tagDtoSetToTagSet(Set<TagDto> set) {
        if ( set == null ) {
            return null;
        }

        Set<Tag> set1 = new LinkedHashSet<Tag>( Math.max( (int) ( set.size() / .75f ) + 1, 16 ) );
        for ( TagDto tagDto : set ) {
            set1.add( tagDtoToTag( tagDto ) );
        }

        return set1;
    }

    protected Book bookSimpleDtoToBook(BookSimpleDto bookSimpleDto) {
        if ( bookSimpleDto == null ) {
            return null;
        }

        Book book = new Book();

        book.setBookId( bookSimpleDto.getBookId() );
        book.setTitle( bookSimpleDto.getTitle() );
        book.setPublicationDate( bookSimpleDto.getPublicationDate() );
        book.setViewsAmount( bookSimpleDto.getViewsAmount() );
        book.setDescription( bookSimpleDto.getDescription() );
        book.setTags( tagDtoSetToTagSet( bookSimpleDto.getTags() ) );

        return book;
    }

    protected Set<Book> bookSimpleDtoSetToBookSet(Set<BookSimpleDto> set) {
        if ( set == null ) {
            return null;
        }

        Set<Book> set1 = new LinkedHashSet<Book>( Math.max( (int) ( set.size() / .75f ) + 1, 16 ) );
        for ( BookSimpleDto bookSimpleDto : set ) {
            set1.add( bookSimpleDtoToBook( bookSimpleDto ) );
        }

        return set1;
    }
}
