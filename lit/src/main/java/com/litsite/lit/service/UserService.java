package com.litsite.lit.service;

import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
    }

    public List<MyUser> allUsers() {
        List<MyUser> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
}
