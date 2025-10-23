package com.ecsite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ecsite.auth.dto.CreateUserRequest;
import com.ecsite.auth.dto.RegistrationResponse;
import com.ecsite.auth.dto.UserResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.exception.UserAlreadyExistsException;
import com.ecsite.auth.mapper.UserMapper;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private BCryptPasswordEncoder passwordEncoder;

  @Mock private JwtUtil jwtUtil;

  @Mock private UserMapper userMapper;

  @InjectMocks private UserRegistrationService userRegistrationService;

  private CreateUserRequest validRequest;
  private User savedUser;

  @BeforeEach
  void setUp() {
    validRequest = new CreateUserRequest();
    validRequest.setEmail("test@example.com");
    validRequest.setPassword("SecurePass123!");
    validRequest.setFirstName("John");
    validRequest.setLastName("Doe");

    savedUser = new User();
    savedUser.setId(UUID.randomUUID());
    savedUser.setEmail("test@example.com");
    savedUser.setPasswordHash("$2a$12$hashedPassword");
    savedUser.setFirstName("John");
    savedUser.setLastName("Doe");
    savedUser.setStatus(User.UserStatus.PENDING);
    savedUser.setCreatedAt(LocalDateTime.now());
    savedUser.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void registerUser_Success() {
    UserResponse userResponse = new UserResponse();
    userResponse.setId(savedUser.getId());
    userResponse.setEmail(savedUser.getEmail());
    userResponse.setFirstName(savedUser.getFirstName());
    userResponse.setLastName(savedUser.getLastName());
    userResponse.setStatus(savedUser.getStatus());
    userResponse.setCreatedAt(savedUser.getCreatedAt());
    userResponse.setUpdatedAt(savedUser.getUpdatedAt());
    userResponse.setEmailVerified(false);

    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(validRequest.getPassword()))
        .thenReturn("$2a$12$hashedPassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);
    when(jwtUtil.generateAccessToken(any(UUID.class), anyString(), anyString()))
        .thenReturn("access-token");
    when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

    RegistrationResponse response = userRegistrationService.registerUser(validRequest);

    assertNotNull(response);
    assertEquals("success", response.getStatus());
    assertEquals("Registration successful", response.getMessage());
    assertNotNull(response.getData());
    assertNotNull(response.getTokens());
    assertEquals("access-token", response.getTokens().getAccessToken());
    assertEquals("refresh-token", response.getTokens().getRefreshToken());
    assertEquals("bearer", response.getTokens().getTokenType());
    assertEquals(900, response.getTokens().getExpiresIn());

    verify(userRepository).existsByEmail(validRequest.getEmail());
    verify(passwordEncoder).encode(validRequest.getPassword());
    verify(userRepository).save(any(User.class));
    verify(jwtUtil).generateAccessToken(any(UUID.class), anyString(), anyString());
    verify(jwtUtil).generateRefreshToken(any(UUID.class));
  }

  @Test
  void registerUser_DuplicateEmail_ThrowsException() {
    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

    assertThrows(
        UserAlreadyExistsException.class,
        () -> userRegistrationService.registerUser(validRequest));

    verify(userRepository).existsByEmail(validRequest.getEmail());
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUser_PasswordIsHashed() {
    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(validRequest.getPassword()))
        .thenReturn("$2a$12$hashedPassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());
    when(jwtUtil.generateAccessToken(any(UUID.class), anyString(), anyString()))
        .thenReturn("access-token");
    when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

    userRegistrationService.registerUser(validRequest);

    verify(passwordEncoder).encode("SecurePass123!");
    verify(userRepository)
        .save(
            argThat(
                user ->
                    user.getPasswordHash().equals("$2a$12$hashedPassword")
                        && !user.getPasswordHash().equals(validRequest.getPassword())));
  }

  @Test
  void registerUser_UserStatusIsPending() {
    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());
    when(jwtUtil.generateAccessToken(any(UUID.class), anyString(), anyString()))
        .thenReturn("access-token");
    when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

    userRegistrationService.registerUser(validRequest);

    verify(userRepository)
        .save(argThat(user -> user.getStatus() == User.UserStatus.PENDING));
  }

  @Test
  void registerUser_EmailVerifiedAtIsNull() {
    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());
    when(jwtUtil.generateAccessToken(any(UUID.class), anyString(), anyString()))
        .thenReturn("access-token");
    when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

    userRegistrationService.registerUser(validRequest);

    verify(userRepository).save(argThat(user -> user.getEmailVerifiedAt() == null));
  }
}
