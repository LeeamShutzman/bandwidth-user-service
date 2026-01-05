package com.bandwidth.userservice.service;

import com.bandwidth.userservice.dto.UserCreateRequestDTO;
import com.bandwidth.userservice.dto.UserResponseDTO;
import com.bandwidth.userservice.dto.UserUpdateRequestDTO;
import com.bandwidth.userservice.model.User;
import com.bandwidth.userservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserKafkaProducerService kafkaProducer;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("createUser - Success")
    void createUser_Success() {
        // Arrange
        UserCreateRequestDTO request = new UserCreateRequestDTO("test@test.com", "user1", "raw_pass");
        User userEntity = new User(1L,"test@test.com", "user1", "raw_pass", true);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw_pass")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(userEntity);

        // Act
        UserResponseDTO response = userService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("user1", response.getUsername());
        verify(kafkaProducer, times(1)).sendUserCreatedEvent(any(User.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - Throws DuplicateUserException for Username")
    void createUser_DuplicateUsername() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("test@test.com", "taken", "pass");
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateUserException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
        verify(kafkaProducer, never()).sendUserCreatedEvent(any());
    }

    @Test
    @DisplayName("createUser - Throws DuplicateUserException for Email")
    void createUser_DuplicateEmail() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("taken@test.com", "validUser", "pass");

        // Username is available, but email is found
        when(userRepository.findByUsername("validUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateUserException.class, () -> userService.createUser(request));
    }

    // --- GET USER TESTS ---

    @Test
    @DisplayName("getUser - Throws EntityNotFoundException")
    void getUser_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.getUser(1L));
    }

    // --- UPDATE USER TESTS ---

    @Test
    @DisplayName("updateUser - Success with Password Change")
    void updateUser_PasswordChange_Success() {
        // Arrange
        User existingUser = new User();
        existingUser.setPasswordHash("old_hash");
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setCurrentPassword("old_raw");
        request.setNewPassword("new_raw");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("old_raw", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("new_raw")).thenReturn("new_hash");
        when(userRepository.save(any())).thenReturn(existingUser);

        // Act
        userService.updateUser(1L, request);

        // Assert
        assertEquals("new_hash", existingUser.getPasswordHash());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUser - Success Email Change")
    void updateUser_EmailChange_Success() {
        User existingUser = new User(1L, "old@test.com", "user1", "hash", true);
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setEmail("new@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(existingUser);

        userService.updateUser(1L, request);

        assertEquals("new@test.com", existingUser.getEmail());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUser - Throws InvalidCredentialsException on wrong password")
    void updateUser_WrongPassword() {
        User existingUser = new User();
        existingUser.setPasswordHash("hashed");
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setCurrentPassword("wrong_raw");
        request.setNewPassword("new_raw");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong_raw", "hashed")).thenReturn(false);

        // Assuming you have this exception class
        assertThrows(InvalidCredentialsException.class, () -> userService.updateUser(1L, request));
    }

    // --- DELETE USER TESTS ---

    @Test
    @DisplayName("deleteUser - Returns true and sends Kafka event")
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

        boolean result = userService.deleteUser(1L);

        assertTrue(result);
        verify(userRepository).deleteById(1L);
        verify(kafkaProducer).sendUserDeletedEvent(1L);
    }

    @Test
    @DisplayName("deleteUser - Returns false if user not found")
    void deleteUser_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = userService.deleteUser(1L);

        assertFalse(result);
        verify(userRepository, never()).deleteById(any());
        verify(kafkaProducer, never()).sendUserDeletedEvent(anyLong());
    }

    @Test
    @DisplayName("getCredentialsByUsername - Success")
    void getCredentials_Success() {
        User user = new User(1L, "test@test.com", "john", "hashed", true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        var result = userService.getCredentialsByUsername("john");

        assertNotNull(result);
        assertEquals("hashed", result.getHashedPassword());
    }
}