package com.prostriver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>No Spring context, Testcontainers, or mocks are used: {@link JwtService}'s
 * only collaborator, {@link SecurityProperties}, is a plain settable POJO that
 * this test constructs directly, and {@code init()} is called manually since
 * this test class lives in the same package as {@link JwtService} and can see
 * its package-private {@code @PostConstruct} method.
 *
 * <p>The "GET ownership -> 404" part of this coverage item (item 3 of the
 * suite build-out) is already covered by
 * {@code StudyPlanControllerTest.getJob_ownedByDifferentUser_returns404} in a
 * separate test class. That test is currently blocked from running by an
 * unrelated pre-existing production issue in {@code ProStriverApplication}'s
 * {@code @EnableJpaRepositories} placement, which is being handled separately
 * — so no duplicate ownership test is added here.
 */
class JwtServiceTest {

    /** 32 ASCII bytes (256 bits), base64-encoded — satisfies the HS256 minimum key length. */
    private static final String VALID_SECRET_BASE64 =
            Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());

    private JwtService buildService(String issuer, String secretBase64, int accessTokenMinutes) {
        SecurityProperties props = new SecurityProperties();
        props.getJwt().setSecretBase64(secretBase64);
        props.getJwt().setIssuer(issuer);
        props.getJwt().setAccessTokenMinutes(accessTokenMinutes);
        JwtService service = new JwtService(props);
        service.init();
        return service;
    }

    @Test
    void generateAndParse_validToken_roundTripsSubjectClaimsAndIssuer() {
        JwtService service = buildService("prostriver-test", VALID_SECRET_BASE64, 15);
        Map<String, Object> claims = Map.of("role", "USER", "userId", "user-123");

        String token = service.generateAccessToken("user-123", claims);
        Jws<Claims> parsed = service.parseAndValidate(token);

        assertThat(parsed.getBody().getSubject()).isEqualTo("user-123");
        assertThat(parsed.getBody().getIssuer()).isEqualTo("prostriver-test");
        assertThat(parsed.getBody().get("role")).isEqualTo("USER");
        assertThat(parsed.getBody().get("userId")).isEqualTo("user-123");
    }

    @Test
    void parseAndValidate_expiredToken_throwsExpiredJwtException() {
        // Negative accessTokenMinutes puts `exp` in the past the instant the token is minted.
        JwtService service = buildService("prostriver-test", VALID_SECRET_BASE64, -1);

        String token = service.generateAccessToken("user-123", Map.of());

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseAndValidate_tamperedSignature_throwsJwtException() {
        JwtService service = buildService("prostriver-test", VALID_SECRET_BASE64, 15);
        String token = service.generateAccessToken("user-123", Map.of());

        int lastDot = token.lastIndexOf('.');
        String headerAndPayload = token.substring(0, lastDot);
        String signature = token.substring(lastDot + 1);

        char lastChar = signature.charAt(signature.length() - 1);
        char replacement = (lastChar == 'A') ? 'B' : 'A';
        String tamperedSignature = signature.substring(0, signature.length() - 1) + replacement;

        String tamperedToken = headerAndPayload + "." + tamperedSignature;

        assertThatThrownBy(() -> service.parseAndValidate(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_wrongIssuer_throwsJwtException() {
        JwtService issuerA = buildService("issuer-A", VALID_SECRET_BASE64, 15);
        JwtService issuerB = buildService("issuer-B", VALID_SECRET_BASE64, 15);

        String tokenFromA = issuerA.generateAccessToken("user-123", Map.of());

        assertThatThrownBy(() -> issuerB.parseAndValidate(tokenFromA))
                .isInstanceOf(JwtException.class);
    }
}
