package com.ProStriver.auth;

import com.ProStriver.auth.dto.*;
import com.ProStriver.auth.util.OtpGenerator;
import com.ProStriver.common.crypto.Sha256;
import com.ProStriver.common.exception.ApiException;
import com.ProStriver.entity.OtpCode;
import com.ProStriver.entity.RefreshToken;
import com.ProStriver.entity.User;
import com.ProStriver.entity.enums.*;
import com.ProStriver.notification.EmailService;
import com.ProStriver.repository.OtpCodeRepository;
import com.ProStriver.repository.RefreshTokenRepository;
import com.ProStriver.repository.UserRepository;
import com.ProStriver.security.JwtService;
import com.ProStriver.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final OtpProperties otpProperties;
    private final EmailService emailService;
    private final Clock clock;

    // ---------- SIGNUP ----------
    @Transactional
    public MessageResponse signup(SignupRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(req.getFullName().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.USER);
        user.setNotificationPreference(NotificationPreference.EMAIL);
        user.setEmailVerified(false);

        userRepository.save(user);

        sendOtpInternal(email, OtpPurpose.SIGNUP_VERIFY_EMAIL);
        return new MessageResponse("Signup created. OTP sent to email.");
    }

    @Transactional
    public MessageResponse resendSignupOtp(String emailRaw) {
        String email = emailRaw.toLowerCase().trim();

        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                sendOtpInternal(email, OtpPurpose.SIGNUP_VERIFY_EMAIL);
            }
        });

        return new MessageResponse("If the email is registered and unverified, an OTP has been sent.");
    }

    @Transactional
    public MessageResponse verifySignupOtp(VerifyOtpRequest req) {
        OtpPurpose purpose = parsePurpose(req.getPurpose());
        if (purpose != OtpPurpose.SIGNUP_VERIFY_EMAIL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid purpose for signup verify");
        }

        String email = req.getEmail().toLowerCase().trim();
        verifyOtpOrThrow(email, purpose, req.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        return new MessageResponse("Email verified successfully.");
    }

    // ---------- LOGIN ----------
    @Transactional
    public AuthTokens login(LoginRequest req, HttpServletRequest httpReq) {
        String email = req.getEmail().toLowerCase().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Email not verified");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                Map.of("uid", user.getId().toString(), "role", user.getRole().name())
        );

        IssuedRefreshToken issued = issueRefreshToken(user, httpReq);

        long expiresIn = securityProperties.getJwt().getAccessTokenMinutes() * 60L;
        return new AuthTokens(accessToken, expiresIn, issued.getRefreshTokenRaw());
    }

    // ---------- REFRESH (rotation) ----------
    @Transactional
    public AuthTokens refresh(String refreshTokenRaw, HttpServletRequest httpReq) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        String hash = Sha256.hex(refreshTokenRaw);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        // If a revoked token is presented again, assume compromise and revoke all active tokens.
        if (existing.getStatus() == RefreshTokenStatus.REVOKED) {
            UUID userId = existing.getUser().getId();
            revokeAllActiveRefreshTokens(userId);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        if (existing.getStatus() != RefreshTokenStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token not active");
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            existing.setStatus(RefreshTokenStatus.EXPIRED);
            refreshTokenRepository.save(existing);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = existing.getUser();

        // revoke old
        existing.setStatus(RefreshTokenStatus.REVOKED);
        existing.setRevokedAt(LocalDateTime.now(clock));

        // issue new
        IssuedRefreshToken replacement = issueRefreshToken(user, httpReq);
        existing.setReplacedByTokenId(replacement.getRefreshTokenId());
        refreshTokenRepository.save(existing);

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                Map.of("uid", user.getId().toString(), "role", user.getRole().name())
        );

        long expiresIn = securityProperties.getJwt().getAccessTokenMinutes() * 60L;
        return new AuthTokens(accessToken, expiresIn, replacement.getRefreshTokenRaw());
    }


    // ---------- LOGOUT ----------
    @Transactional
    public MessageResponse logout(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            return new MessageResponse("Logged out.");
        }

        refreshTokenRepository.findByTokenHash(Sha256.hex(refreshTokenRaw)).ifPresent(rt -> {
            rt.setStatus(RefreshTokenStatus.REVOKED);
            rt.setRevokedAt(LocalDateTime.now(clock));
            refreshTokenRepository.save(rt);
        });

        return new MessageResponse("Logged out.");
    }

    @Transactional
    public MessageResponse logoutAllDevices(String emailRaw) {
        String email = emailRaw.toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndStatus(user.getId(), RefreshTokenStatus.ACTIVE);
        if (!tokens.isEmpty()) {
            LocalDateTime now = LocalDateTime.now(clock);
            tokens.forEach(rt -> {
                rt.setStatus(RefreshTokenStatus.REVOKED);
                rt.setRevokedAt(now);
            });
            refreshTokenRepository.saveAll(tokens);
        }

        return new MessageResponse("Logged out from all devices.");
    }

    // ---------- FORGOT / RESET PASSWORD ----------
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        // Do not leak existence; only send if user exists + verified
        userRepository.findByEmail(email).ifPresent(u -> {
            if (u.isEmailVerified()) {
                sendOtpInternal(email, OtpPurpose.FORGOT_PASSWORD);
            }
        });

        return new MessageResponse("If the email exists, an OTP has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        verifyOtpOrThrow(email, OtpPurpose.FORGOT_PASSWORD, req.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // revoke all active refresh tokens
        revokeAllActiveRefreshTokens(user.getId());

        return new MessageResponse("Password reset successful.");
    }

    @Transactional
    public MessageResponse changePassword(String emailRaw, ChangePasswordRequest req) {
        String email = emailRaw.toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // revoke all active refresh tokens
        revokeAllActiveRefreshTokens(user.getId());

        return new MessageResponse("Password changed successfully.");
    }

    // ---------- helpers ----------
    private void revokeAllActiveRefreshTokens(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndStatus(userId, RefreshTokenStatus.ACTIVE);
        if (tokens.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now(clock);
        tokens.forEach(rt -> {
            rt.setStatus(RefreshTokenStatus.REVOKED);
            rt.setRevokedAt(now);
        });
        refreshTokenRepository.saveAll(tokens);
    }

    private void sendOtpInternal(String email, OtpPurpose purpose) {
        LocalDateTime now = LocalDateTime.now(clock);

        OtpCode latest = otpCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose).orElse(null);

        int cooldownSeconds = otpProperties.getResendCooldownSeconds();
        if (latest != null && latest.getLastSentAt() != null &&
                latest.getLastSentAt().plusSeconds(cooldownSeconds).isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting OTP again");
        }

        String otp = OtpGenerator.generate6Digits();

        int ttlMinutes = (purpose == OtpPurpose.SIGNUP_VERIFY_EMAIL)
                ? otpProperties.getSignupTtlMinutes()
                : otpProperties.getForgotPasswordTtlMinutes();

        OtpCode code = OtpCode.builder()
                .email(email)
                .purpose(purpose)
                .otpHash(Sha256.hex(otp))
                .expiresAt(now.plusMinutes(ttlMinutes))
                .attempts(0)
                .maxAttempts(otpProperties.getMaxAttempts())
                .used(false)
                .lastSentAt(now)
                .build();

        otpCodeRepository.save(code);

        String subject = (purpose == OtpPurpose.SIGNUP_VERIFY_EMAIL)
                ? "ProStriver - Verify your email"
                : "ProStriver - Reset your password";

        emailService.sendOtp(email, subject, otp);
    }

    private void verifyOtpOrThrow(String email, OtpPurpose purpose, String otpRaw) {
        OtpCode code = otpCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid OTP"));

        if (code.isUsed()) throw new ApiException(HttpStatus.UNAUTHORIZED, "OTP already used");
        if (code.getExpiresAt().isBefore(LocalDateTime.now(clock))) throw new ApiException(HttpStatus.UNAUTHORIZED, "OTP expired");
        if (code.getAttempts() >= code.getMaxAttempts()) throw new ApiException(HttpStatus.UNAUTHORIZED, "Too many attempts");

        code.setAttempts(code.getAttempts() + 1);

        if (!Sha256.hex(otpRaw).equals(code.getOtpHash())) {
            otpCodeRepository.save(code);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }

        code.setUsed(true);
        otpCodeRepository.save(code);
    }

    private OtpPurpose parsePurpose(String p) {
        try {
            return OtpPurpose.valueOf(p);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid purpose");
        }
    }

    private IssuedRefreshToken issueRefreshToken(User user, HttpServletRequest httpReq) {
        String refreshRaw = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(Sha256.hex(refreshRaw));
        token.setStatus(RefreshTokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now(clock).plusDays(securityProperties.getJwt().getRefreshTokenDays()));
        token.setUserAgent(httpReq.getHeader("User-Agent"));
        token.setIpAddress(resolveIp(httpReq));

        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(token.getId(), refreshRaw);
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}