package com.litsite.lit.service;

import com.litsite.lit.dto.JwtAuthenticationDto;
import com.litsite.lit.dto.RefreshTokenDto;
import com.litsite.lit.dto.UserCredentialsDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public List<MyUser> allUsers() {
        List<MyUser> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public JwtAuthenticationDto signIn(UserCredentialsDto userCredentialsDto) throws AuthenticationException {
        MyUser user = findByCredentials(userCredentialsDto);
        return jwtService.generateAuthToken(user.getEmail());
    }

    public JwtAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto) throws Exception {
        String refreshToken = refreshTokenDto.getRefreshToken();
        if (refreshToken != null && jwtService.validateJwtToken(refreshToken)) {
            MyUser user = findByEmail(jwtService.getEmailFromToken(refreshToken));
            return jwtService.refreshBaseToken(user.getEmail(), refreshToken);
        }
        throw new AuthenticationException("Invalid refresh token");
    }

    public String addUser(UserDto userDto) {
        MyUser user = userMapper.toEntity(userDto);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        userRepository.save(user);
        return "User added";
    }

    private MyUser findByCredentials(UserCredentialsDto userCredentialsDto) throws AuthenticationException {
        Optional<MyUser> optionalMyUser = userRepository.findByEmail(userCredentialsDto.getEmail());
        if (optionalMyUser.isPresent()) {
            MyUser user = optionalMyUser.get();
            if (passwordEncoder.matches(userCredentialsDto.getPassword(), user.getPasswordHash())) {
                return user;
            }
        }
        throw new AuthenticationException("Email or password is invalid");
    }

    private MyUser findByEmail(String email) throws Exception {
        return userRepository.findByEmail(email).orElseThrow(() -> new Exception(String.format("User with emal is not found", email)));
    }
}
