package com.bandwidth.userservice.integration;

import com.bandwidth.userservice.dto.UserCreateRequestDTO;
import com.bandwidth.userservice.dto.UserResponseDTO;
import com.bandwidth.userservice.model.User;
import com.bandwidth.userservice.repository.UserRepository;
import com.bandwidth.userservice.service.UserKafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test") // This tells Spring to load application-test.properties
@TestPropertySource(properties = "internal.api.key=my-secret-token") // Fixes potential @Value errors
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserKafkaProducerService kafkaProducer; // Still mock external infrastructure

    @Test
    @DisplayName("Scenario: Create a user and then retrieve them (End-to-End)")
    void createAndGetUser_IntegrationScenario() throws Exception {
        // 1. Arrange: Prepare a request
        UserCreateRequestDTO request = new UserCreateRequestDTO("alice@example.com", "alice_wonder", "password123");

        // 2. Act: Create the user via the API
        String responseJson = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice_wonder"))
                .andReturn().getResponse().getContentAsString();

        UserResponseDTO createdUser = objectMapper.readValue(responseJson, UserResponseDTO.class);
        Long userId = createdUser.getId();

        // 3. Act: Immediately try to get that same user from the API
        mockMvc.perform(get("/api/v1/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        // 4. Assert: Verify the user is actually in the database
        assertTrue(userRepository.findByUsername("alice_wonder").isPresent());

        // 5. Assert: Verify the Kafka event was triggered
        verify(kafkaProducer, times(1)).sendUserCreatedEvent(any());
    }

    @Test
    @DisplayName("Scenario: Prevent duplicate registration across the whole stack")
    void duplicateRegistration_IntegrationScenario() throws Exception {
        // Pre-insert a user into the real database
        userRepository.save(new User(null, "bob@test.com", "bob_builder", "hashed", true));

        UserCreateRequestDTO duplicateRequest = new UserCreateRequestDTO("bob@test.com", "new_bob", "pass");

        // Act & Assert: This proves the Repository's findByEmail works with the Service's logic
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }
}