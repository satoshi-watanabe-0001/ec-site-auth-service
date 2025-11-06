package com.ecsite.auth.controller;

import com.ecsite.auth.dto.CreateUserRequest;
import com.ecsite.auth.dto.EmailVerificationRequest;
import com.ecsite.auth.dto.EmailVerificationResponse;
import com.ecsite.auth.dto.LoginRequest;
import com.ecsite.auth.dto.LoginResponse;
import com.ecsite.auth.dto.MemberRegistrationRequest;
import com.ecsite.auth.dto.RegistrationResponse;
import com.ecsite.auth.exception.UserAlreadyExistsException;
import com.ecsite.auth.service.EmailVerificationService;
import com.ecsite.auth.service.LoginService;
import com.ecsite.auth.service.UserRegistrationService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UserRegistrationService userRegistrationService;
  private final EmailVerificationService emailVerificationService;
  private final LoginService loginService;

  /**
   * 既存の会員登録エンドポイント
   *
   * @param request 会員登録リクエスト（email, password, firstName, lastName形式）
   * @return 登録結果レスポンス
   */
  @PostMapping("/auth/register")
  public ResponseEntity<RegistrationResponse> register(
      @Valid @RequestBody CreateUserRequest request) {
    log.info("Registration request received for email: {}", request.getEmail());
    RegistrationResponse response = userRegistrationService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * EC-11: 会員登録API
   *
   * <p>チケット仕様に基づく会員登録エンドポイント。 {name, description, status}形式のリクエストを受け付けます。
   *
   * @param request 会員登録リクエスト（name, description, status形式）
   * @return 登録結果レスポンス
   */
  @PostMapping("/会員登録")
  public ResponseEntity<RegistrationResponse> registerMember(
      @Valid @RequestBody MemberRegistrationRequest request) {
    log.info("Member registration request received for name: {}", request.getName());
    RegistrationResponse response = userRegistrationService.registerMember(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * メールアドレス認証エンドポイント
   *
   * <p>メール認証トークンを検証し、ユーザーのメールアドレスを認証済みにします。
   *
   * @param request メール認証リクエスト
   * @return 認証結果レスポンス
   */
  @PostMapping("/auth/verify-email")
  public ResponseEntity<EmailVerificationResponse> verifyEmail(
      @Valid @RequestBody EmailVerificationRequest request) {
    log.info("Email verification request received for token: {}", request.getToken());

    boolean verified = emailVerificationService.verifyEmail(request.getToken());

    EmailVerificationResponse response =
        EmailVerificationResponse.builder()
            .status("success")
            .message("Email verified successfully")
            .build();

    return ResponseEntity.ok(response);
  }

  /**
   * EC-13: ログインAPI
   *
   * <p>メールアドレスとパスワードによる認証を行い、JWTトークンを発行します。
   *
   * @param request ログインリクエスト（email, password形式）
   * @return ログイン結果レスポンス（トークンとユーザー情報）
   */
  @PostMapping("/auth/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("Login request received for email: {}", request.getEmail());
    LoginResponse response = loginService.authenticateUser(request);
    return ResponseEntity.ok(response);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
    log.warn("Authentication failed: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(
      UserAlreadyExistsException ex) {
    log.warn("User already exists exception: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", "Validation failed");
    errorResponse.put(
        "errors",
        Map.of(
            "field", "email",
            "message", "Email already exists"));
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", "Validation failed");

    var errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        error.getField(),
                        "message",
                        error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Invalid value"))
            .collect(Collectors.toList());

    errorResponse.put("errors", errors);
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred", ex);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", "An unexpected error occurred");
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
