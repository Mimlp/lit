package com.litsite.lit.mapper;

import com.litsite.lit.dto.TagDto;
import com.litsite.lit.models.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {
    TagDto tagToTagDto(Tag tag);

    Set<TagDto> tagToTagDtoSet(Set<Tag> tags);

    List<TagDto> tagToTagDtoList(List<Tag> tags);

    Tag tagDtoToTag(TagDto tagDto);

    Set<Tag> tagDtoToTagSet(Set<TagDto> tags);
}
