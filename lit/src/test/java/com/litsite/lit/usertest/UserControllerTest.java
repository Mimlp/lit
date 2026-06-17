package com.litsite.lit.usertest;

import com.litsite.lit.controller.AuthHelper;
import com.litsite.lit.controller.UserController;
import com.litsite.lit.dto.AddBookDto;
import com.litsite.lit.dto.UpdateProfileDto;
import com.litsite.lit.dto.UserDto;
import com.litsite.lit.exception.UserNotFoundException;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        public AuthHelper authHelper() { return mock(AuthHelper.class); }
    }

    @Autowired private UserController userController;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthHelper authHelper;

    private MyUser testUser;

    @BeforeEach void before() {
        testUser = new MyUser();
        testUser.setUsername("testuser"); testUser.setEmail("test@example.com");
        testUser.setPasswordHash("pass"); testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setProfileDescription("Initial");
        testUser = userRepository.save(testUser);

        lenient().when(authHelper.getCurrentUserOrThrow()).thenReturn(testUser);
        lenient().when(authHelper.getCurrentUserId()).thenReturn(testUser.getUserId());
    }

    @AfterEach
    void after() {
        bookRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test @Transactional void testAuthenticatedUser() {
        ResponseEntity<UserDto> res = userController.authenticatedUser();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(testUser.getUserId(), res.getBody().getUserId());
    }

    @Test @Transactional void testUpdateProfile_Success() {
        UpdateProfileDto dto = new UpdateProfileDto();
        dto.setUserName("up"); dto.setProfileDescription("upd");
        ResponseEntity<UserDto> res = userController.updateProfile(dto);
        assertEquals("up", res.getBody().getUsername());
    }

    @Test @Transactional void testUpdateProfile_Partial() {
        UpdateProfileDto dto = new UpdateProfileDto(); dto.setUserName("new");
        ResponseEntity<UserDto> res = userController.updateProfile(dto);
        assertEquals("new", res.getBody().getUsername());
    }

    @Test @Transactional void testAddWork_Success() {
        AddBookDto dto = new AddBookDto(); dto.setTitle("NB"); dto.setDescription("D");
        ResponseEntity<Long> res = userController.addWork(dto);
        assertNotNull(res.getBody());
    }

    @Test @Transactional void testAllUsers() {
        ResponseEntity<List<UserDto>> res = userController.allUsers();
        assertTrue(res.getBody().size() >= 1);
    }

    @Test @Transactional void testGetUser_Success() {
        ResponseEntity<?> res = userController.getUser(testUser.getUserId());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}