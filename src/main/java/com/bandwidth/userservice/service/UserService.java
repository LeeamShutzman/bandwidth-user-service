package com.bandwidth.userservice.service;

import com.bandwidth.userservice.model.User;
import com.bandwidth.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public User createUser(User user) {
        // VALIDATION: Check for unique email and username ---
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new DuplicateUserException("email", user.getEmail());
        });

        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new DuplicateUserException("username", user.getUsername());
        });
        // --------------------------------------------------------

        // TODO: In a later step, we will use a dedicated PasswordEncoder here to hash the user.getPasswordHash()

        // 1. Save the User entity to the database
        User savedUser = userRepository.save(user);

        // 2. Publish the event to notify other services (e.g., Task Service)
        userKafkaProducerService.sendUserCreatedEvent(savedUser);

        return savedUser;
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

    // You can add methods for findById, findAll, etc.
}