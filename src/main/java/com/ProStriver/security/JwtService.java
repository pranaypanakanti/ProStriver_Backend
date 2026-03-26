package com.ProStriver.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecurityProperties securityProperties;
    private Key cachedSigningKey;

    @PostConstruct
    void init() {
        byte[] decoded = Base64.getDecoder().decode(securityProperties.getJwt().getSecretBase64());
        this.cachedSigningKey = Keys.hmacShaKeyFor(decoded);
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getAccessTokenMinutes() * 60L);

        return Jwts.builder()
                .setIssuer(securityProperties.getJwt().getIssuer())
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(claims)
                .signWith(cachedSigningKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(cachedSigningKey)
                .requireIssuer(securityProperties.getJwt().getIssuer())
                .build()
                .parseClaimsJws(token);
    }
}