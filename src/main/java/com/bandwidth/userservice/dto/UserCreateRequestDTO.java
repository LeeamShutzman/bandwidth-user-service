package com.bandwidth.userservice.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserCreateRequestDTO implements Serializable {
    @NotNull
    private String email;
    @NotNull
    private String username;
    @NotNull
    private String password;
}
