package com.bandwidth.userservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserCredentialDTO {
    private Long id;
    private String username;
    private String hashedPassword;
    private List<String> roles; // e.g., ["USER", "ADMIN"]
}