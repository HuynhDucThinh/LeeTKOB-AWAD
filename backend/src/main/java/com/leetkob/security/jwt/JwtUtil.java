package com.leetkob.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private static final int MIN_SECRET_BYTES = 32;

  private final SecretKey key;
  private final long expirationMs;

  public JwtUtil(
      @Value("${jwt.secret:${JWT_SECRET:}}") String secret,
      @Value("${jwt.expiration-ms:${JWT_EXPIRATION_MS:86400000}}") long expirationMs) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException(
          "JWT_SECRET (jwt.secret) must be configured and at least 32 bytes long.");
    }
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < MIN_SECRET_BYTES) {
      throw new IllegalArgumentException(
          "JWT_SECRET (jwt.secret) must be at least 32 bytes long for HS256.");
    }
    this.key = Keys.hmacShaKeyFor(secretBytes);
    this.expirationMs = expirationMs;
  }

  public String generateToken(UUID userId, String email) {
    return generateToken(userId, email, "student");
  }

  public String generateToken(UUID userId, String email, String role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);
    String safeRole = (role == null || role.isBlank()) ? "student" : role.trim().toLowerCase();

    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("role", safeRole)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key)
        .compact();
  }

  public UUID getUserIdFromToken(String token) {
    Claims claims = getClaims(token);
    return UUID.fromString(claims.getSubject());
  }

  public String getEmailFromToken(String token) {
    Claims claims = getClaims(token);
    return claims.get("email", String.class);
  }

  public String getRoleFromToken(String token) {
    Claims claims = getClaims(token);
    String role = claims.get("role", String.class);
    return (role == null || role.isBlank()) ? "student" : role;
  }

  public boolean validateToken(String token) {
    try {
      Claims claims = getClaims(token);
      return !claims.getExpiration().before(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
