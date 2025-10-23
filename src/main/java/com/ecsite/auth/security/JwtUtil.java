package com.ecsite.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(UUID userId, String email, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", userId.toString());
    claims.put("email", email);
    claims.put("role", role);
    claims.put("type", "ACCESS");
    claims.put("jti", UUID.randomUUID().toString());

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateRefreshToken(UUID userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", userId.toString());
    claims.put("type", "REFRESH");
    claims.put("jti", UUID.randomUUID().toString());

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateEmailVerificationToken(UUID userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", userId.toString());
    claims.put("email", email);
    claims.put("type", "EMAIL_VERIFICATION");
    claims.put("jti", UUID.randomUUID().toString());

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims validateToken(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  public UUID getUserIdFromToken(String token) {
    Claims claims = validateToken(token);
    return UUID.fromString(claims.getSubject());
  }

  public String getTokenType(String token) {
    Claims claims = validateToken(token);
    return claims.get("type", String.class);
  }

  public boolean isTokenExpired(String token) {
    try {
      Claims claims = validateToken(token);
      return claims.getExpiration().before(new Date());
    } catch (Exception e) {
      return true;
    }
  }
}
