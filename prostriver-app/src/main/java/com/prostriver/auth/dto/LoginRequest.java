package com.prostriver.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @Email
    @NotBlank
    @Pattern(regexp = "^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "Email must have a valid domain (e.g., user@example.com)")
    private String email;

    @NotBlank
    private String password;
}