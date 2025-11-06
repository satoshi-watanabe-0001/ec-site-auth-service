package com.ecsite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecsite.auth.entity.PasswordResetToken;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.PasswordResetTokenRepository;
import com.ecsite.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private PasswordResetTokenRepository tokenRepository;

  @Mock private UserRepository userRepository;

  @Mock private NotificationService notificationService;

  @Mock private BCryptPasswordEncoder passwordEncoder;

  @InjectMocks private PasswordResetService passwordResetService;

  private User testUser;
  private PasswordResetToken testToken;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setEmail("test@example.com");
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setStatus(User.UserStatus.ACTIVE);
    testUser.setPasswordHash("$2a$12$oldHashedPassword");

    testToken = new PasswordResetToken();
    testToken.setId(UUID.randomUUID());
    testToken.setToken("test-reset-token-123");
    testToken.setUser(testUser);
    testToken.setExpiresAt(LocalDateTime.now().plusHours(24));
    testToken.setCreatedAt(LocalDateTime.now());
  }

  @Test
  void generatePasswordResetToken_UserExists_Success() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

    String message = passwordResetService.generatePasswordResetToken("test@example.com");

    assertNotNull(message);
    assertEquals("If the email exists, a password reset link has been sent", message);
    verify(userRepository).findByEmail("test@example.com");
    verify(tokenRepository).save(any(PasswordResetToken.class));
    verify(notificationService).sendPasswordResetEmail(anyString(), anyString());
  }

  @Test
  void generatePasswordResetToken_UserNotFound_ReturnsSameMessage() {
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    String message = passwordResetService.generatePasswordResetToken("nonexistent@example.com");

    assertNotNull(message);
    assertEquals("If the email exists, a password reset link has been sent", message);
    verify(userRepository).findByEmail("nonexistent@example.com");
    verify(tokenRepository, never()).save(any(PasswordResetToken.class));
    verify(notificationService, never()).sendPasswordResetEmail(anyString(), anyString());
  }

  @Test
  void resetPassword_ValidToken_Success() {
    when(tokenRepository.findByToken("test-reset-token-123")).thenReturn(Optional.of(testToken));
    when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("$2a$12$newHashedPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

    boolean result =
        passwordResetService.resetPassword("test-reset-token-123", "NewSecurePassword123!");

    assertTrue(result);
    assertEquals("$2a$12$newHashedPassword", testUser.getPasswordHash());
    assertNotNull(testToken.getUsedAt());
    verify(tokenRepository).findByToken("test-reset-token-123");
    verify(passwordEncoder).encode("NewSecurePassword123!");
    verify(userRepository).save(testUser);
    verify(tokenRepository).save(testToken);
  }

  @Test
  void resetPassword_InvalidToken_ThrowsException() {
    when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword("invalid-token", "NewPassword123!"));

    verify(tokenRepository).findByToken("invalid-token");
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void resetPassword_ExpiredToken_ThrowsException() {
    testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
    when(tokenRepository.findByToken("test-reset-token-123")).thenReturn(Optional.of(testToken));

    assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword("test-reset-token-123", "NewPassword123!"));

    verify(tokenRepository).findByToken("test-reset-token-123");
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void resetPassword_AlreadyUsedToken_ThrowsException() {
    testToken.setUsedAt(LocalDateTime.now().minusHours(1));
    when(tokenRepository.findByToken("test-reset-token-123")).thenReturn(Optional.of(testToken));

    assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword("test-reset-token-123", "NewPassword123!"));

    verify(tokenRepository).findByToken("test-reset-token-123");
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }
}
