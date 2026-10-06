package com.pricepulse.auth.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private static final long EXPIRY_HOURS = 24;

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // HMAC-SHA keys require at least 256 bits (32 bytes) for HS256.
        // application.yml enforces a minimum length via the default placeholder.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issue a signed HS256 JWT whose subject is the user's numeric id.
     * The token expires 24 hours from the moment of creation.
     */
    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRY_HOURS, ChronoUnit.HOURS)))
                .signWith(key)          // JJWT 0.12 infers HS256 from the key type
                .compact();
    }

    /**
     * Extract the user id from a valid, unexpired token.
     * Callers should invoke {@link #isValid(String)} first; this method
     * throws if the token is invalid or expired.
     */
    public Instant extractIssuedAt(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt()
                .toInstant();
    }

    public Long extractUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    /**
     * Return {@code true} if the token is parseable, has a valid HS256 signature,
     * and has not expired. Never throws — all exceptions are caught and mapped to
     * {@code false} so that the JWT filter can safely call this without try/catch.
     */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Covers: signature mismatch, expired token, malformed token, null/blank subject
            return false;
        }
    }
}
