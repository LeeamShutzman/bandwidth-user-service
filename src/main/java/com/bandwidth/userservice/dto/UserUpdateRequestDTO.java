package com.bandwidth.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateRequestDTO implements Serializable {
    @Email(message = "Email must be valid.")
    private String email;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    private String username;

    private String currentPassword;

    @Size(min = 8, max = 128, message = "New password must be at least 8 characters long.")
    private String newPassword;

    /**
     * Helper method to determine if the DTO contains a password change request.
     */
    public boolean isPasswordChangeRequested() {
        // A password change is requested only if BOTH currentPassword and newPassword are provided.
        // The service layer must then verify currentPassword against the stored hash.
        return this.currentPassword != null && !this.currentPassword.isBlank() &&
                this.newPassword != null && !this.newPassword.isBlank();
    }
}
