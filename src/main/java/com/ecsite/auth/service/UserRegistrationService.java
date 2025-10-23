package com.ecsite.auth.service;

import com.ecsite.auth.dto.AuthTokenResponse;
import com.ecsite.auth.dto.CreateUserRequest;
import com.ecsite.auth.dto.RegistrationResponse;
import com.ecsite.auth.dto.UserResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.exception.UserAlreadyExistsException;
import com.ecsite.auth.mapper.UserMapper;
import com.ecsite.auth.repository.UserRepository;
import com.ecsite.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final JwtUtil jwtUtil;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public RegistrationResponse registerUser(CreateUserRequest request) {
    log.info("Starting user registration for email: {}", request.getEmail());

    if (userRepository.existsByEmail(request.getEmail())) {
      log.warn("Registration failed: Email already exists - {}", request.getEmail());
      throw new UserAlreadyExistsException("Email already exists");
    }

    String hashedPassword = passwordEncoder.encode(request.getPassword());

    User user =
        User.builder()
            .email(request.getEmail())
            .passwordHash(hashedPassword)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .status(User.UserStatus.PENDING)
            .build();

    User savedUser = userRepository.save(user);
    log.info("User created successfully with ID: {}", savedUser.getId());

    String accessToken =
        jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail(), "USER");

    String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

    AuthTokenResponse tokens =
        AuthTokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("bearer")
            .expiresIn(900)
            .build();

    UserResponse userResponse = userMapper.toUserResponse(savedUser);

    return RegistrationResponse.builder()
        .status("success")
        .message("Registration successful")
        .data(userResponse)
        .tokens(tokens)
        .build();
  }
}
