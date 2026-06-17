package com.litsite.lit.mapper;

import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.models.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringList")
    UserDto toDto(MyUser user);

<<<<<<< Updated upstream
    List<UserDto> toDto(List<MyUser> user);
=======
    List<UserDto> toDto(List<MyUser> users);
>>>>>>> Stashed changes

    AuthorDto toAuthor(MyUser user);

    @Named("rolesToStringList")
    default List<String> rolesToStringList(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .map(Role::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}