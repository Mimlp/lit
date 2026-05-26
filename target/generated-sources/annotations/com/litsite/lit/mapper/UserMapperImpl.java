package com.litsite.lit.mapper;

import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.models.MyUser;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-02T20:42:59+0400",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(MyUser user) {
        if ( user == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setUserId( user.getUserId() );
        userDto.setUsername( user.getUsername() );
        userDto.setEmail( user.getEmail() );
        userDto.setProfileDescription( user.getProfileDescription() );
        userDto.setRegistrationDate( user.getRegistrationDate() );
        userDto.setIsEnabled( user.getIsEnabled() );
        userDto.setRoles( user.getRoles() );

        return userDto;
    }

    @Override
    public List<UserDto> toDto(List<MyUser> user) {
        if ( user == null ) {
            return null;
        }

        List<UserDto> list = new ArrayList<UserDto>( user.size() );
        for ( MyUser myUser : user ) {
            list.add( toDto( myUser ) );
        }

        return list;
    }

    @Override
    public AuthorDto toAuthor(MyUser user) {
        if ( user == null ) {
            return null;
        }

        AuthorDto authorDto = new AuthorDto();

        authorDto.setUserId( user.getUserId() );
        authorDto.setUsername( user.getUsername() );
        authorDto.setProfileDescription( user.getProfileDescription() );

        return authorDto;
    }
}
