package com.litsite.lit.usertest;

import com.litsite.lit.dto.AuthorDto;
import com.litsite.lit.dto.UpdateProfileDto;
import com.litsite.lit.exception.UserNotFoundException;
import com.litsite.lit.mapper.UserMapper;
import com.litsite.lit.models.Book;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.repository.BookRepository;
import com.litsite.lit.repository.UserRepository;
import com.litsite.lit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private MyUser testUser;
    private AuthorDto testAuthorDto;

    @BeforeEach
    void setUp() {
        testUser = new MyUser();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("originaluser");
        testUser.setProfileDescription("Original desc");

        testAuthorDto = new AuthorDto();
        testAuthorDto.setUserId(1L);
        testAuthorDto.setUsername("originaluser");
    }

    @Test
    void testAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<MyUser> result = userService.allUsers();
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    void testGetUser_Found() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        MyUser result = userService.getUser("test@example.com");
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetUser_NotFound_ReturnsEmptyUser() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        MyUser result = userService.getUser("unknown@example.com");
        assertNotNull(result);
        assertNull(result.getUserId());
        assertNull(result.getUsername());
    }

    @Test
    void testGetAuthor_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toAuthor(testUser)).thenReturn(testAuthorDto);
        AuthorDto result = userService.getAuthor(1L);
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("originaluser", result.getUsername());
        verify(userMapper).toAuthor(testUser);
    }

    @Test
    void testGetAuthor_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getAuthor(999L));
    }

    @Test
    void testUpdateProfile_FullUpdate() {
        UpdateProfileDto dto = new UpdateProfileDto();
        dto.setUserName("newname");
        dto.setProfileDescription("newdesc");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(MyUser.class))).thenReturn(testUser);

        MyUser result = userService.updateProfile("test@example.com", dto);
        assertEquals("newname", result.getUsername());
        assertEquals("newdesc", result.getProfileDescription());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateProfile_PartialUpdate_OnlyUsername() {
        UpdateProfileDto dto = new UpdateProfileDto();
        dto.setUserName("onlyname");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(MyUser.class))).thenReturn(testUser);

        MyUser result = userService.updateProfile("test@example.com", dto);
        assertEquals("onlyname", result.getUsername());
        assertEquals("Original desc", result.getProfileDescription());
    }

    @Test
    void testUpdateProfile_PartialUpdate_OnlyDescription() {
        UpdateProfileDto dto = new UpdateProfileDto();
        dto.setProfileDescription("onlydesc");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(MyUser.class))).thenReturn(testUser);

        MyUser result = userService.updateProfile("test@example.com", dto);
        assertEquals("originaluser", result.getUsername());
        assertEquals("onlydesc", result.getProfileDescription());
    }

    @Test
    void testUpdateProfile_NullFields_NoChanges() {
        UpdateProfileDto dto = new UpdateProfileDto();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(MyUser.class))).thenReturn(testUser);

        MyUser result = userService.updateProfile("test@example.com", dto);
        assertEquals("originaluser", result.getUsername());
        assertEquals("Original desc", result.getProfileDescription());
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.updateProfile("unknown@example.com", new UpdateProfileDto()));
    }

    @Test
    void testSaveBook() {
        Book book = new Book();
        book.setBookId(10L);
        when(bookRepository.save(book)).thenReturn(book);
        Book result = userService.saveBook(book);
        assertNotNull(result);
        assertEquals(10L, result.getBookId());
        verify(bookRepository).save(book);
    }
}