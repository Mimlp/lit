package com.litsite.lit.mapper;

import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.models.MyUser;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserDto toDto(MyUser user);
    List<UserDto> toDto(List<MyUser> user);
    AuthorDto toAuthor(MyUser user);
}
