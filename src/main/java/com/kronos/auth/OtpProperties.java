package com.kronos.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kronos.otp")
public class OtpProperties {
    private int signupTtlMinutes;
    private int forgotPasswordTtlMinutes;
    private int resendCooldownSeconds;
    private int maxAttempts;
}