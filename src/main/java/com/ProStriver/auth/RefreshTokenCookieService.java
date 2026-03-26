package com.ProStriver.auth;

import com.ProStriver.security.SecurityProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenCookieService {

    private final SecurityProperties securityProperties;

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshTokenRaw, int refreshTokenDays) {
        ResponseCookie cookie = ResponseCookie.from(securityProperties.getCookies().getRefreshTokenName(), refreshTokenRaw)
                .httpOnly(true)
                .secure(securityProperties.getCookies().isRefreshTokenSecure())
                .path(securityProperties.getCookies().getRefreshTokenPath())
                .sameSite(securityProperties.getCookies().getRefreshTokenSameSite())
                .maxAge(Duration.ofDays(refreshTokenDays))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(securityProperties.getCookies().getRefreshTokenName(), "")
                .httpOnly(true)
                .secure(securityProperties.getCookies().isRefreshTokenSecure())
                .path(securityProperties.getCookies().getRefreshTokenPath())
                .sameSite(securityProperties.getCookies().getRefreshTokenSameSite())
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}