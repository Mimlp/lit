package com.litsite.lit.service;

import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.UpdateProfileDto;
import com.litsite.lit.exception.UserNotFoundException;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final UserMapper userMapper;

    public List<MyUser> allUsers() {
        List<MyUser> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public MyUser getUser(String email) {
        Optional<MyUser> optionalMyUser = userRepository.findByEmail(email);
        MyUser user = new MyUser();
        if (optionalMyUser.isPresent()) {
            user = optionalMyUser.get();
        }
        return user;
    }

    public AuthorDto getAuthor(Integer id) {
        Optional<MyUser> myUser = userRepository.findById(id);
        if (myUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }
        return userMapper.toAuthor(myUser.get());
    }

    public MyUser updateProfile(String email, UpdateProfileDto updateProfileDto) {
        MyUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (updateProfileDto.getProfileDescription() != null) {
            user.setProfileDescription(updateProfileDto.getProfileDescription());
        }
        if (updateProfileDto.getUserName() != null) {
            user.setUsername(updateProfileDto.getUserName());
        }
        return userRepository.save(user);
    }

    public MyUser saveUser(MyUser myUser) {
        return userRepository.save(myUser);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }
}
