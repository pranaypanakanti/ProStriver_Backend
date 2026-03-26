package com.ProStriver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokens {
    private String accessToken;
    private long expiresInSeconds;
    private String refreshTokenRaw;
}