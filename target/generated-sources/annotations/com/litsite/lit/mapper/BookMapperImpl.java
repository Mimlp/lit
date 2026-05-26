package com.litsite.lit.mapper;

import com.litsite.lit.dto.BookDto;
import com.litsite.lit.dto.BookSimpleDto;
import com.litsite.lit.dto.ChapterDto;
import com.litsite.lit.dto.CreateChapterDto;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.Chapter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-02T20:42:59+0400",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class BookMapperImpl implements BookMapper {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    public BookDto bookToBookDto(Book book, Long currentUserId) {
        if ( book == null ) {
            return null;
        }

        BookDto bookDto = new BookDto();

        bookDto.setAuthor( userMapper.toAuthor( book.getUser() ) );
        bookDto.setTags( tagMapper.tagToTagDtoSet( book.getTags() ) );
        bookDto.setBookId( book.getBookId() );
        bookDto.setTitle( book.getTitle() );
        bookDto.setDescription( book.getDescription() );
        bookDto.setPublicationDate( book.getPublicationDate() );
        bookDto.setViewsAmount( book.getViewsAmount() );
        bookDto.setChapters( chapterToChapterDtoList( book.getChapters() ) );

        applyUserRights( book, bookDto, currentUserId );

        return bookDto;
    }

    @Override
    public BookSimpleDto bookToBookSimpleDto(Book book) {
        if ( book == null ) {
            return null;
        }

        BookSimpleDto bookSimpleDto = new BookSimpleDto();

        bookSimpleDto.setAuthor( userMapper.toAuthor( book.getUser() ) );
        bookSimpleDto.setTags( tagMapper.tagToTagDtoSet( book.getTags() ) );
        bookSimpleDto.setBookId( book.getBookId() );
        bookSimpleDto.setTitle( book.getTitle() );
        bookSimpleDto.setDescription( book.getDescription() );
        bookSimpleDto.setPublicationDate( book.getPublicationDate() );
        bookSimpleDto.setViewsAmount( book.getViewsAmount() );

        return bookSimpleDto;
    }

    @Override
    public List<BookSimpleDto> booksToBookSimpleDtos(List<Book> books) {
        if ( books == null ) {
            return null;
        }

        List<BookSimpleDto> list = new ArrayList<BookSimpleDto>( books.size() );
        for ( Book book : books ) {
            list.add( bookToBookSimpleDto( book ) );
        }

        return list;
    }

    @Override
    public Book bookDtoToBook(BookDto bookDto) {
        if ( bookDto == null ) {
            return null;
        }

        Book book = new Book();

        book.setBookId( bookDto.getBookId() );
        book.setTitle( bookDto.getTitle() );
        book.setPublicationDate( bookDto.getPublicationDate() );
        book.setViewsAmount( bookDto.getViewsAmount() );
        book.setDescription( bookDto.getDescription() );
        book.setChapters( chapterDtoListToChapterList( bookDto.getChapters() ) );
        book.setTags( tagMapper.tagDtoToTagSet( bookDto.getTags() ) );

        return book;
    }

    @Override
    public ChapterDto chapterToChapterDto(Chapter chapter) {
        if ( chapter == null ) {
            return null;
        }

        ChapterDto chapterDto = new ChapterDto();

        chapterDto.setChapterId( chapter.getChapterId() );
        chapterDto.setChapterNumber( chapter.getChapterNumber() );
        chapterDto.setChapterText( chapter.getChapterText() );
        chapterDto.setChapterTitle( chapter.getChapterTitle() );

        return chapterDto;
    }

    @Override
    public Chapter chapterDtoToChapter(ChapterDto chapterDto) {
        if ( chapterDto == null ) {
            return null;
        }

        Chapter chapter = new Chapter();

        chapter.setChapterId( chapterDto.getChapterId() );
        chapter.setChapterNumber( chapterDto.getChapterNumber() );
        chapter.setChapterText( chapterDto.getChapterText() );
        chapter.setChapterTitle( chapterDto.getChapterTitle() );

        return chapter;
    }

    @Override
    public List<ChapterDto> chapterToChapterDtoList(List<Chapter> chapters) {
        if ( chapters == null ) {
            return null;
        }

        List<ChapterDto> list = new ArrayList<ChapterDto>( chapters.size() );
        for ( Chapter chapter : chapters ) {
            list.add( chapterToChapterDto( chapter ) );
        }

        return list;
    }

    @Override
    public Chapter createChapterDtotoChapter(CreateChapterDto chapterDto) {
        if ( chapterDto == null ) {
            return null;
        }

        Chapter chapter = new Chapter();

        chapter.setChapterText( chapterDto.getChapterText() );
        chapter.setChapterTitle( chapterDto.getChapterTitle() );

        return chapter;
    }

    @Override
    public Set<BookSimpleDto> booksToBookSimpleDtos(Set<Book> books) {
        if ( books == null ) {
            return null;
        }

        Set<BookSimpleDto> set = new LinkedHashSet<BookSimpleDto>( Math.max( (int) ( books.size() / .75f ) + 1, 16 ) );
        for ( Book book : books ) {
            set.add( bookToBookSimpleDto( book ) );
        }

        return set;
    }

    protected List<Chapter> chapterDtoListToChapterList(List<ChapterDto> list) {
        if ( list == null ) {
            return null;
        }

        List<Chapter> list1 = new ArrayList<Chapter>( list.size() );
        for ( ChapterDto chapterDto : list ) {
            list1.add( chapterDtoToChapter( chapterDto ) );
        }

        return list1;
    }
}
