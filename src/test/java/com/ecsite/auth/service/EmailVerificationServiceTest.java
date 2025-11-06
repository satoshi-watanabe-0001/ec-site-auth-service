package com.ecsite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecsite.auth.entity.EmailVerificationToken;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.EmailVerificationTokenRepository;
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

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  @Mock private EmailVerificationTokenRepository tokenRepository;

  @Mock private UserRepository userRepository;

  @Mock private NotificationService notificationService;

  @InjectMocks private EmailVerificationService emailVerificationService;

  private User testUser;
  private EmailVerificationToken testToken;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setEmail("test@example.com");
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setStatus(User.UserStatus.PENDING);
    testUser.setPasswordHash("$2a$12$hashedPassword");

    testToken = new EmailVerificationToken();
    testToken.setId(UUID.randomUUID());
    testToken.setToken("test-token-123");
    testToken.setUser(testUser);
    testToken.setExpiresAt(LocalDateTime.now().plusHours(24));
    testToken.setCreatedAt(LocalDateTime.now());
  }

  @Test
  void generateVerificationToken_Success() {
    when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);

    String token = emailVerificationService.generateVerificationToken(testUser);

    assertNotNull(token);
    verify(tokenRepository).save(any(EmailVerificationToken.class));
    verify(notificationService).sendVerificationEmail(testUser.getEmail(), token);
  }

  @Test
  void verifyEmail_Success() {
    when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);

    boolean result = emailVerificationService.verifyEmail("test-token-123");

    assertTrue(result);
    assertNotNull(testUser.getEmailVerifiedAt());
    assertEquals(User.UserStatus.ACTIVE, testUser.getStatus());
    verify(tokenRepository).findByToken("test-token-123");
    verify(userRepository).save(testUser);
    verify(tokenRepository).save(testToken);
  }

  @Test
  void verifyEmail_InvalidToken_ThrowsException() {
    when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> emailVerificationService.verifyEmail("invalid-token"));

    verify(tokenRepository).findByToken("invalid-token");
  }

  @Test
  void verifyEmail_ExpiredToken_ThrowsException() {
    testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
    when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

    assertThrows(
        IllegalArgumentException.class,
        () -> emailVerificationService.verifyEmail("test-token-123"));

    verify(tokenRepository).findByToken("test-token-123");
  }

  @Test
  void verifyEmail_AlreadyVerifiedToken_ThrowsException() {
    testToken.setVerifiedAt(LocalDateTime.now());
    when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));

    assertThrows(
        IllegalArgumentException.class,
        () -> emailVerificationService.verifyEmail("test-token-123"));

    verify(tokenRepository).findByToken("test-token-123");
  }

  @Test
  void verifyEmail_UserAlreadyActive_DoesNotChangeStatus() {
    testUser.setStatus(User.UserStatus.ACTIVE);
    when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);

    emailVerificationService.verifyEmail("test-token-123");

    assertEquals(User.UserStatus.ACTIVE, testUser.getStatus());
    verify(userRepository).save(testUser);
  }

  @Test
  void resendVerificationToken_Success() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);

    String token = emailVerificationService.resendVerificationToken("test@example.com");

    assertNotNull(token);
    verify(userRepository).findByEmail("test@example.com");
    verify(tokenRepository).save(any(EmailVerificationToken.class));
    verify(notificationService).sendVerificationEmail(anyString(), anyString());
  }

  @Test
  void resendVerificationToken_UserNotFound_ThrowsException() {
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> emailVerificationService.resendVerificationToken("nonexistent@example.com"));

    verify(userRepository).findByEmail("nonexistent@example.com");
  }

  @Test
  void resendVerificationToken_EmailAlreadyVerified_ThrowsException() {
    testUser.setEmailVerifiedAt(LocalDateTime.now());
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    assertThrows(
        IllegalArgumentException.class,
        () -> emailVerificationService.resendVerificationToken("test@example.com"));

    verify(userRepository).findByEmail("test@example.com");
  }
}
