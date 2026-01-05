package com.bandwidth.userservice.service;

import com.bandwidth.userservice.dto.UserCreateRequestDTO;
import com.bandwidth.userservice.dto.UserCredentialDTO;
import com.bandwidth.userservice.dto.UserResponseDTO;
import com.bandwidth.userservice.dto.UserUpdateRequestDTO;
import com.bandwidth.userservice.model.User;
import com.bandwidth.userservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserKafkaProducerService userKafkaProducerService;
    private final PasswordEncoder passwordEncoder; // Injected for security

    public UserService(UserRepository userRepository, UserKafkaProducerService userKafkaProducerService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userKafkaProducerService = userKafkaProducerService;
        this.passwordEncoder = passwordEncoder; // Assigned
    }

    /****************************************************************************************/

    @Transactional
    public UserResponseDTO createUser(UserCreateRequestDTO requestDTO) {
        // VALIDATION: Check for unique username and username ---
        userRepository.findByUsername(requestDTO.getUsername()).ifPresent(u -> {
            throw new DuplicateUserException("username", requestDTO.getUsername());
        });

        userRepository.findByEmail(requestDTO.getEmail()).ifPresent(u -> {
            throw new DuplicateUserException("email", requestDTO.getEmail());
        });
        // --------------------------------------------------------

        //requestDTO.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        // 1. Save the User entity to the database
        User savedUser = userRepository.save(convertDtoToEntity(requestDTO));

        // 2. Publish the event to notify other services (e.g., Task Service)
        userKafkaProducerService.sendUserCreatedEvent(savedUser);

        return UserResponseDTO.fromEntity(savedUser);
    }

    public UserResponseDTO getUser(@PathVariable Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        return UserResponseDTO.fromEntity(user);
    }

    /****************************************************************************************/

    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO requestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        if (requestDTO.isPasswordChangeRequested()) {
            // A. Verify the current password
            boolean isCurrentPasswordValid = passwordEncoder.matches(
                    requestDTO.getCurrentPassword(),
                    user.getPasswordHash()
            );

            if (!isCurrentPasswordValid) {
                throw new InvalidCredentialsException("The provided current password is not correct.");
            }

            String newHashedPassword = passwordEncoder.encode(requestDTO.getNewPassword());
            user.setPasswordHash(newHashedPassword);
        }

        // Update Username
        if (requestDTO.getUsername() != null && !requestDTO.getUsername().isBlank() && !requestDTO.getUsername().equals(user.getUsername())) {
            // Check if the new username is already in use
            userRepository.findByUsername(requestDTO.getUsername()).ifPresent(u -> {
                throw new DuplicateUserException("username", requestDTO.getUsername());
            });
            user.setUsername(requestDTO.getUsername());
        }

        // Update Username
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isBlank() && !requestDTO.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(requestDTO.getEmail()).ifPresent(u -> {
                throw new DuplicateUserException("email", requestDTO.getEmail());
            });
            user.setEmail(requestDTO.getEmail());
        }
        User savedUser = userRepository.save(user);

        // publish a 'UserUpdatedEvent' here.

        return UserResponseDTO.fromEntity(savedUser);
    }

    /****************************************************************************************/

    @Transactional
    public boolean deleteUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isPresent()) {
            // 1. Delete the User from the database
            userRepository.deleteById(userId);

            // 2. Publish the event. This triggers the cascading delete in the Task Service.
            userKafkaProducerService.sendUserDeletedEvent(userId);

            return true;
        }
        return false;
    }

    /****************************************************************************************/

    /** Utils **/

    private User convertDtoToEntity(UserCreateRequestDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        user.setPasswordHash(hashedPassword);
        return user;
    }


    public UserCredentialDTO getCredentialsByUsername(String username) {
        // 1. Use findByUsername and handle absence gracefully (return null)
        return userRepository.findByUsername(username)
                .map(user -> UserCredentialDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .hashedPassword(user.getPasswordHash())
                        .roles(List.of("ROLE_USER"))
                        .build())
                        .orElse(null);
    }
}