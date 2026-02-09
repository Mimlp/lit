package com.litsite.lit.service;

import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    public UserService(UserRepository userRepository, BookRepository bookRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

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

    public MyUser updateProfileDescription(String email, String description) {
        MyUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfileDescription(description);
        return userRepository.save(user);
    }

    public MyUser saveUser(MyUser myUser) {
        return userRepository.save(myUser);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }
}
