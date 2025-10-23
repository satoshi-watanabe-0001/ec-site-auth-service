package com.ecsite.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

  private JwtUtil jwtUtil;
  private UUID testUserId;
  private String testEmail;
  private String testRole;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(
        jwtUtil, "secret", "test-secret-key-that-is-long-enough-for-hs256-algorithm");
    ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L); // 15 minutes
    ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L); // 30 days

    testUserId = UUID.randomUUID();
    testEmail = "test@example.com";
    testRole = "USER";
  }

  @Test
  void generateAccessToken_Success() {
    String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

    assertNotNull(token);
    assertFalse(token.isEmpty());

    Claims claims = jwtUtil.validateToken(token);
    assertEquals(testUserId.toString(), claims.getSubject());
    assertEquals(testEmail, claims.get("email", String.class));
    assertEquals(testRole, claims.get("role", String.class));
    assertEquals("ACCESS", claims.get("type", String.class));
    assertNotNull(claims.get("jti"));
  }

  @Test
  void generateRefreshToken_Success() {
    String token = jwtUtil.generateRefreshToken(testUserId);

    assertNotNull(token);
    assertFalse(token.isEmpty());

    Claims claims = jwtUtil.validateToken(token);
    assertEquals(testUserId.toString(), claims.getSubject());
    assertEquals("REFRESH", claims.get("type", String.class));
    assertNotNull(claims.get("jti"));
  }

  @Test
  void generateEmailVerificationToken_Success() {
    String token = jwtUtil.generateEmailVerificationToken(testUserId, testEmail);

    assertNotNull(token);
    assertFalse(token.isEmpty());

    Claims claims = jwtUtil.validateToken(token);
    assertEquals(testUserId.toString(), claims.getSubject());
    assertEquals(testEmail, claims.get("email", String.class));
    assertEquals("EMAIL_VERIFICATION", claims.get("type", String.class));
    assertNotNull(claims.get("jti"));
  }

  @Test
  void validateToken_Success() {
    String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

    Claims claims = jwtUtil.validateToken(token);

    assertNotNull(claims);
    assertEquals(testUserId.toString(), claims.getSubject());
  }

  @Test
  void getUserIdFromToken_Success() {
    String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

    UUID userId = jwtUtil.getUserIdFromToken(token);

    assertEquals(testUserId, userId);
  }

  @Test
  void getTokenType_AccessToken() {
    String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

    String tokenType = jwtUtil.getTokenType(token);

    assertEquals("ACCESS", tokenType);
  }

  @Test
  void getTokenType_RefreshToken() {
    String token = jwtUtil.generateRefreshToken(testUserId);

    String tokenType = jwtUtil.getTokenType(token);

    assertEquals("REFRESH", tokenType);
  }

  @Test
  void isTokenExpired_NotExpired() {
    String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

    boolean expired = jwtUtil.isTokenExpired(token);

    assertFalse(expired);
  }

  @Test
  void isTokenExpired_InvalidToken() {
    String invalidToken = "invalid.token.here";

    boolean expired = jwtUtil.isTokenExpired(invalidToken);

    assertTrue(expired);
  }
}
