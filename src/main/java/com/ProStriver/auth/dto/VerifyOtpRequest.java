package com.ProStriver.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String purpose; // SIGNUP_VERIFY_EMAIL / FORGOT_PASSWORD

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String otp;
}