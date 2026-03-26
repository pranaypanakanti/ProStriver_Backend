package com.ProStriver.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ProStriver.otp")
public class OtpProperties {
    private int signupTtlMinutes;
    private int forgotPasswordTtlMinutes;
    private int resendCooldownSeconds;
    private int maxAttempts;
}