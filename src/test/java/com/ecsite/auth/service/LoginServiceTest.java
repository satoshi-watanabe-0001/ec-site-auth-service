package com.ecsite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecsite.auth.dto.LoginRequest;
import com.ecsite.auth.dto.LoginResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.UserRepository;
import com.ecsite.auth.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private BCryptPasswordEncoder passwordEncoder;

  @Mock private JwtUtil jwtUtil;

  @InjectMocks private LoginService loginService;

  private LoginRequest validRequest;
  private User activeUser;

  @BeforeEach
  void setUp() {
    validRequest = new LoginRequest();
    validRequest.setEmail("test@example.com");
    validRequest.setPassword("SecurePass123!");
    validRequest.setRememberMe(false);

    activeUser = new User();
    activeUser.setId(UUID.randomUUID());
    activeUser.setEmail("test@example.com");
    activeUser.setPasswordHash("$2a$12$hashedPassword");
    activeUser.setFirstName("John");
    activeUser.setLastName("Doe");
    activeUser.setStatus(User.UserStatus.ACTIVE);
    activeUser.setCreatedAt(LocalDateTime.now());
    activeUser.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void authenticateUser_Success() {
    when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(activeUser));
    when(passwordEncoder.matches(validRequest.getPassword(), activeUser.getPasswordHash()))
        .thenReturn(true);
    when(jwtUtil.generateAccessToken(any(UUID.class), anyString(), anyString()))
        .thenReturn("access-token");
    when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

    LoginResponse response = loginService.authenticateUser(validRequest);

    assertNotNull(response);
    assertEquals("access-token", response.getAccessToken());
    assertEquals("refresh-token", response.getRefreshToken());
    assertEquals("bearer", response.getTokenType());
    assertEquals(900, response.getExpiresIn());
    assertNotNull(response.getUser());
    assertEquals(activeUser.getId().toString(), response.getUser().getId());
    assertEquals(activeUser.getEmail(), response.getUser().getEmail());
    assertEquals(1, response.getUser().getRoles().length);
    assertEquals("user", response.getUser().getRoles()[0]);
    assertEquals(false, response.getUser().isMfaEnabled());

    verify(userRepository).findByEmail(validRequest.getEmail());
    verify(passwordEncoder).matches(validRequest.getPassword(), activeUser.getPasswordHash());
    verify(jwtUtil).generateAccessToken(activeUser.getId(), activeUser.getEmail(), "USER");
    verify(jwtUtil).generateRefreshToken(activeUser.getId());
  }

  @Test
  void authenticateUser_UserNotFound_ThrowsBadCredentialsException() {
    when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());

    BadCredentialsException exception =
        assertThrows(
            BadCredentialsException.class, () -> loginService.authenticateUser(validRequest));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(userRepository).findByEmail(validRequest.getEmail());
  }

  @Test
  void authenticateUser_InvalidPassword_ThrowsBadCredentialsException() {
    when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(activeUser));
    when(passwordEncoder.matches(validRequest.getPassword(), activeUser.getPasswordHash()))
        .thenReturn(false);

    BadCredentialsException exception =
        assertThrows(
            BadCredentialsException.class, () -> loginService.authenticateUser(validRequest));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(userRepository).findByEmail(validRequest.getEmail());
    verify(passwordEncoder).matches(validRequest.getPassword(), activeUser.getPasswordHash());
  }

  @Test
  void authenticateUser_PendingUser_ThrowsBadCredentialsException() {
    User pendingUser = new User();
    pendingUser.setId(UUID.randomUUID());
    pendingUser.setEmail("test@example.com");
    pendingUser.setPasswordHash("$2a$12$hashedPassword");
    pendingUser.setStatus(User.UserStatus.PENDING);

    when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(pendingUser));
    when(passwordEncoder.matches(validRequest.getPassword(), pendingUser.getPasswordHash()))
        .thenReturn(true);

    BadCredentialsException exception =
        assertThrows(
            BadCredentialsException.class, () -> loginService.authenticateUser(validRequest));

    assertEquals(
        "Account is not active. Please verify your email or contact support.",
        exception.getMessage());
    verify(userRepository).findByEmail(validRequest.getEmail());
    verify(passwordEncoder).matches(validRequest.getPassword(), pendingUser.getPasswordHash());
  }

  @Test
  void authenticateUser_InactiveUser_ThrowsBadCredentialsException() {
    User inactiveUser = new User();
    inactiveUser.setId(UUID.randomUUID());
    inactiveUser.setEmail("test@example.com");
    inactiveUser.setPasswordHash("$2a$12$hashedPassword");
    inactiveUser.setStatus(User.UserStatus.INACTIVE);

    when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(inactiveUser));
    when(passwordEncoder.matches(validRequest.getPassword(), inactiveUser.getPasswordHash()))
        .thenReturn(true);

    BadCredentialsException exception =
        assertThrows(
            BadCredentialsException.class, () -> loginService.authenticateUser(validRequest));

    assertEquals(
        "Account is not active. Please verify your email or contact support.",
        exception.getMessage());
    verify(userRepository).findByEmail(validRequest.getEmail());
    verify(passwordEncoder).matches(validRequest.getPassword(), inactiveUser.getPasswordHash());
  }

  @Test
  void authenticateUser_SuspendedUser_ThrowsBadCredentialsException() {
    User suspendedUser = new User();
    suspendedUser.setId(UUID.randomUUID());
    suspendedUser.setEmail("test@example.com");
    suspendedUser.setPasswordHash("$2a$12$hashedPassword");
    suspendedUser.setStatus(User.UserStatus.SUSPENDED);

    when(userRepository.findByEmail(validRequest.getEmail()))
        .thenReturn(Optional.of(suspendedUser));
    when(passwordEncoder.matches(validRequest.getPassword(), suspendedUser.getPasswordHash()))
        .thenReturn(true);

    BadCredentialsException exception =
        assertThrows(
            BadCredentialsException.class, () -> loginService.authenticateUser(validRequest));

    assertEquals(
        "Account is not active. Please verify your email or contact support.",
        exception.getMessage());
    verify(userRepository).findByEmail(validRequest.getEmail());
    verify(passwordEncoder).matches(validRequest.getPassword(), suspendedUser.getPasswordHash());
  }
}
