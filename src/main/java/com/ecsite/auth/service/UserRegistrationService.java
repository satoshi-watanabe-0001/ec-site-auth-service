package com.ecsite.auth.service;

import com.ecsite.auth.dto.AuthTokenResponse;
import com.ecsite.auth.dto.CreateUserRequest;
import com.ecsite.auth.dto.MemberRegistrationRequest;
import com.ecsite.auth.dto.RegistrationResponse;
import com.ecsite.auth.dto.UserResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.exception.UserAlreadyExistsException;
import com.ecsite.auth.mapper.UserMapper;
import com.ecsite.auth.repository.EmailVerificationTokenRepository;
import com.ecsite.auth.repository.UserRepository;
import com.ecsite.auth.security.JwtUtil;
import java.util.UUID;
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
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final UserMapper userMapper;
  private final JwtUtil jwtUtil;
  private final BCryptPasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;

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

  /**
   * EC-11: 会員登録処理（name, description, status形式）
   *
   * <p>チケット仕様に基づく会員登録を実行します。 メール認証トークンを生成し、Notification Serviceに通知を送信します。
   *
   * @param request 会員登録リクエスト
   * @return 登録結果レスポンス
   * @throws UserAlreadyExistsException 会員名が既に存在する場合
   */
  @Transactional
  public RegistrationResponse registerMember(MemberRegistrationRequest request) {
    log.info("Starting member registration for name: {}", request.getName());

    if (userRepository.existsByEmail(request.getName())) {
      log.warn("Registration failed: Name already exists - {}", request.getName());
      throw new UserAlreadyExistsException("Name already exists");
    }

    User.UserStatus userStatus;
    try {
      userStatus = User.UserStatus.valueOf(request.getStatus().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid status provided: {}, defaulting to PENDING", request.getStatus());
      userStatus = User.UserStatus.PENDING;
    }

    String temporaryPassword = UUID.randomUUID().toString();
    String hashedPassword = passwordEncoder.encode(temporaryPassword);

    User user =
        User.builder()
            .email(request.getName())
            .passwordHash(hashedPassword)
            .firstName(request.getName())
            .lastName(request.getDescription() != null ? request.getDescription() : "")
            .status(userStatus)
            .build();

    User savedUser = userRepository.save(user);
    log.info("Member created successfully with ID: {}", savedUser.getId());

    String verificationToken = emailVerificationService.generateVerificationToken(savedUser);
    log.info("Email verification token generated for user: {}", savedUser.getId());

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
        .message("Member registration successful. Please verify your email.")
        .data(userResponse)
        .tokens(tokens)
        .build();
  }
}
