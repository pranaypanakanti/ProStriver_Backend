package com.ProStriver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class IssuedRefreshToken {
    private UUID refreshTokenId;
    private String refreshTokenRaw;
}