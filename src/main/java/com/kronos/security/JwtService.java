package com.kronos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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

    private Key signingKey() {
        byte[] decoded = Base64.getDecoder().decode(securityProperties.getJwt().getSecretBase64());
        return Keys.hmacShaKeyFor(decoded);
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
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .requireIssuer(securityProperties.getJwt().getIssuer())
                .build()
                .parseClaimsJws(token);
    }
}