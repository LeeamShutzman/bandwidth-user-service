package com.bandwidth.userservice.controller;

import com.bandwidth.userservice.config.SecurityConfig;
import com.bandwidth.userservice.dto.UserCreateRequestDTO;
import com.bandwidth.userservice.dto.UserCredentialDTO;
import com.bandwidth.userservice.dto.UserResponseDTO;
import com.bandwidth.userservice.dto.UserUpdateRequestDTO;
import com.bandwidth.userservice.service.DuplicateUserException;
import com.bandwidth.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "internal.api.key=my-secret-token")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // Orchestrates the HTTP requests

    @MockBean
    private UserService userService; // Mocks the business logic layer

    @Autowired
    private ObjectMapper objectMapper; // Converts objects to JSON strings

    @Test
    @DisplayName("POST /api/v1/users - Success")
    void createUser_ShouldReturn201_WhenValidRequest() throws Exception {
        // Arrange
        UserCreateRequestDTO request = new UserCreateRequestDTO("john@example.com", "john_doe", "password123");
        UserResponseDTO response = new UserResponseDTO(1L, "john@example.com", "john_doe");

        when(userService.createUser(any(UserCreateRequestDTO.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Conflict (Duplicate User)")
    void createUser_ShouldReturn409_WhenUserAlreadyExists() throws Exception {
        // Arrange
        when(userService.createUser(any())).thenThrow(new DuplicateUserException("Username already taken"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateRequestDTO())))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already taken"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Success")
    void getUser_ShouldReturn200_WhenUserExists() throws Exception {
        // Arrange
        UserResponseDTO response = new UserResponseDTO(1L, "john@example.com", "john_doe");
        when(userService.getUser(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Not Found")
    void getUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        // If your service returns null or throws an exception, test it here
        when(userService.getUser(99L)).thenThrow(new EntityNotFoundException("User not found with ID: 99"));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Success")
    void updateUser_ShouldReturn200_WhenUserExists() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO("john@example.com", "john_doe", "password123", "password456");
        UserResponseDTO response = new UserResponseDTO(1L, "john@example.com", "john_doe");

        when(userService.updateUser(1L, request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Not Found")
    void updateUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO("email@test.com", "user", "p1", "p2");

        when(userService.updateUser(eq(99L), any(UserUpdateRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(put("/api/v1/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Success")
    void deleteUser_ShouldReturn204_WhenUserDeleted() throws Exception {
        // Arrange
        when(userService.deleteUser(1L)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Not Found")
    void deleteUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        // Arrange
        when(userService.deleteUser(99L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/internal/credentials - Success")
    void getCredentials_ShouldReturn200_WhenValid() throws Exception {
        // Arrange
        UserCredentialDTO credentials = new UserCredentialDTO(1L, "john_doe", "hashed_pass", Collections.singletonList("ROLE_USER"));
        when(userService.getCredentialsByUsername("john_doe")).thenReturn(credentials);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/internal/credentials")
                        .header("X-Internal-Secret", "my-secret-token")
                        .param("username", "john_doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hashedPassword").value("hashed_pass"));
    }

    @Test
    @DisplayName("GET /api/v1/users/internal/credentials - Not Found")
    void getCredentials_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        when(userService.getCredentialsByUsername("unknown_user")).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/internal/credentials")
                        .header("X-Internal-Secret", "my-secret-token")
                        .param("username", "unknown_user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/internal/credentials - Forbidden (Invalid Token)")
    void getCredentials_ShouldReturn403_WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/users/internal/credentials")
                        .header("X-Internal-Secret", "wrong-secret-token")
                        .param("username", "john_doe"))
                .andExpect(status().isForbidden());
    }
}
