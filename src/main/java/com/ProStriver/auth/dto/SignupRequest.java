package com.ProStriver.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @Email
    @NotBlank
    @Pattern(regexp = "^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "Email must have a valid domain (e.g., user@example.com)")
    private String email;

    @NotBlank
    @Size(min = 2, max = 80)
    private String fullName;

    @NotBlank
    @Size(min = 3, max = 72)
    private String password;
}