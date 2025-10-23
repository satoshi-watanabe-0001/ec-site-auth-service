package com.ecsite.auth.controller;

import com.ecsite.auth.dto.CreateUserRequest;
import com.ecsite.auth.dto.RegistrationResponse;
import com.ecsite.auth.exception.UserAlreadyExistsException;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UserRegistrationService userRegistrationService;

  @PostMapping("/register")
  public ResponseEntity<RegistrationResponse> register(
      @Valid @RequestBody CreateUserRequest request) {
    log.info("Registration request received for email: {}", request.getEmail());
    RegistrationResponse response = userRegistrationService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
