package com.prostriver.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @Email
    @NotBlank
    @Pattern(regexp = "^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "Email must have a valid domain (e.g., user@example.com)")
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String otp;

    @NotBlank
    @Size(min = 3, max = 72)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String newPassword;
}