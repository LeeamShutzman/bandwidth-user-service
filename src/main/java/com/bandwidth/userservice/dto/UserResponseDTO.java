package com.bandwidth.userservice.dto;

import com.bandwidth.userservice.model.User;
import lombok.Data;
import java.io.Serializable;

@Data
public class UserResponseDTO implements Serializable {

    private Long id;
    private String email;
    private String username;

    public static UserResponseDTO fromEntity(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }
}
