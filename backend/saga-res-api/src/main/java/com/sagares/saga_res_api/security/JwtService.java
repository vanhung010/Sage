package com.sagares.saga_res_api.security;


import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64URL.decode(properties.secret());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "JWT_SECRET must be valid Base64", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET phải chứa ít nhất 256 bits");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Account account) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(account.getId().toString())
                .claim("role", account.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public JwtClaims parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        String roleClaim = claims.get("role", String.class);

        if (subject == null || roleClaim == null) {
            throw new JwtException("Missing required JWT claims");
        }

        try {
            return new JwtClaims(
                    Long.valueOf(subject),
                    AccountRole.valueOf(roleClaim)
            );
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid JWT claims", e);
        }
    }

    public long getExpiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public record JwtClaims(
            Long accountId,
            AccountRole role
    ) {
    }
}