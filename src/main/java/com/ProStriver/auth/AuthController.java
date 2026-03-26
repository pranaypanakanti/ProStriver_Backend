package com.ProStriver.auth;

import com.ProStriver.auth.dto.*;
import com.ProStriver.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService cookieService;
    private final SecurityProperties securityProperties;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.ok(authService.signup(req));
    }

    @PostMapping("/signup/resend-otp")
    public ResponseEntity<MessageResponse> resendSignupOtp(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendSignupOtp(email));
    }

    @PostMapping("/signup/verify-otp")
    public ResponseEntity<MessageResponse> verifySignupOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(authService.verifySignupOtp(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq,
            HttpServletResponse httpResp
    ) {
        AuthTokens tokens = authService.login(req, httpReq);

        cookieService.setRefreshTokenCookie(httpResp, tokens.getRefreshTokenRaw(), securityProperties.getJwt().getRefreshTokenDays());

        return ResponseEntity.ok(new AuthResponse(tokens.getAccessToken(), "Bearer", tokens.getExpiresInSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String refreshRaw = readCookie(httpReq, securityProperties.getCookies().getRefreshTokenName());

        AuthTokens tokens = authService.refresh(refreshRaw, httpReq);

        cookieService.setRefreshTokenCookie(httpResp, tokens.getRefreshTokenRaw(), securityProperties.getJwt().getRefreshTokenDays());

        return ResponseEntity.ok(new AuthResponse(tokens.getAccessToken(), "Bearer", tokens.getExpiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String refreshRaw = readCookie(httpReq, securityProperties.getCookies().getRefreshTokenName());
        MessageResponse msg = authService.logout(refreshRaw);
        cookieService.clearRefreshTokenCookie(httpResp);
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(Authentication authentication, HttpServletResponse httpResp) {
        MessageResponse msg = authService.logoutAllDevices(authentication.getName());
        cookieService.clearRefreshTokenCookie(httpResp);
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.changePassword(authentication.getName(), req));
    }

    private String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}