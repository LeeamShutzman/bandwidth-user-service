package com.bandwidth.userservice.dto;

import com.bandwidth.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class UserResponseDTO implements Serializable {

    private Long id;
    private String email;
    private String username;

    public static UserResponseDTO fromEntity(User user) {
        UserResponseDTO dto = new UserResponseDTO(user.getId(),user.getEmail(),user.getUsername());
        return dto;
    }
}
