package com.litsite.lit.mapper;

import com.litsite.lit.dto.TagDto;
import com.litsite.lit.models.Tag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
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
public class TagMapperImpl implements TagMapper {

    @Override
    public TagDto tagToTagDto(Tag tag) {
        if ( tag == null ) {
            return null;
        }

        TagDto tagDto = new TagDto();

        tagDto.setTagId( tag.getTagId() );
        tagDto.setTagName( tag.getTagName() );

        return tagDto;
    }

    @Override
    public Set<TagDto> tagToTagDtoSet(Set<Tag> tags) {
        if ( tags == null ) {
            return null;
        }

        Set<TagDto> set = new LinkedHashSet<TagDto>( Math.max( (int) ( tags.size() / .75f ) + 1, 16 ) );
        for ( Tag tag : tags ) {
            set.add( tagToTagDto( tag ) );
        }

        return set;
    }

    @Override
    public List<TagDto> tagToTagDtoList(List<Tag> tags) {
        if ( tags == null ) {
            return null;
        }

        List<TagDto> list = new ArrayList<TagDto>( tags.size() );
        for ( Tag tag : tags ) {
            list.add( tagToTagDto( tag ) );
        }

        return list;
    }

    @Override
    public Tag tagDtoToTag(TagDto tagDto) {
        if ( tagDto == null ) {
            return null;
        }

        Tag tag = new Tag();

        tag.setTagId( tagDto.getTagId() );
        tag.setTagName( tagDto.getTagName() );

        return tag;
    }

    @Override
    public Set<Tag> tagDtoToTagSet(Set<TagDto> tags) {
        if ( tags == null ) {
            return null;
        }

        Set<Tag> set = new LinkedHashSet<Tag>( Math.max( (int) ( tags.size() / .75f ) + 1, 16 ) );
        for ( TagDto tagDto : tags ) {
            set.add( tagDtoToTag( tagDto ) );
        }

        return set;
    }
}
