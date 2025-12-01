package com.bandwidth.userservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a user in the application. This entity maps to the 'app_user' table
 * and will be joined to the 'task' table via the 'id' (userId) primary key.
 */
@Entity
@Table(name = "bandwidth_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // The primary key (equivalent to your 'userid')

    @Column(unique = true, nullable = false)
    private String email; // Unique identifier, can be used for resetting account

    @Column(unique = true, nullable = false)
    private String username; // A display name that can be used for login

    @Column(nullable = false)
    private String passwordHash; // Stores the Bcrypt hash of the password

    private boolean enabled = true; // For future account management (e.g., email verification)
}