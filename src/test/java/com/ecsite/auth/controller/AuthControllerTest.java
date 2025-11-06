package com.ecsite.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecsite.auth.config.SecurityConfig;
import com.ecsite.auth.dto.AuthTokenResponse;
import com.ecsite.auth.dto.CreateUserRequest;
import com.ecsite.auth.dto.RegistrationResponse;
import com.ecsite.auth.dto.UserResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.exception.UserAlreadyExistsException;
import com.ecsite.auth.service.EmailVerificationService;
import com.ecsite.auth.service.UserRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private UserRegistrationService userRegistrationService;

  @MockBean private EmailVerificationService emailVerificationService;

  private CreateUserRequest validRequest;
  private RegistrationResponse successResponse;

  @BeforeEach
  void setUp() {
    validRequest = new CreateUserRequest();
    validRequest.setEmail("test@example.com");
    validRequest.setPassword("SecurePass123!");
    validRequest.setFirstName("John");
    validRequest.setLastName("Doe");

    UserResponse userResponse = new UserResponse();
    userResponse.setId(UUID.randomUUID());
    userResponse.setEmail("test@example.com");
    userResponse.setFirstName("John");
    userResponse.setLastName("Doe");
    userResponse.setStatus(User.UserStatus.PENDING);
    userResponse.setCreatedAt(LocalDateTime.now());
    userResponse.setUpdatedAt(LocalDateTime.now());
    userResponse.setEmailVerified(false);

    AuthTokenResponse tokenResponse = new AuthTokenResponse();
    tokenResponse.setAccessToken("access-token");
    tokenResponse.setRefreshToken("refresh-token");
    tokenResponse.setTokenType("bearer");
    tokenResponse.setExpiresIn(900);

    successResponse = new RegistrationResponse();
    successResponse.setStatus("success");
    successResponse.setMessage("Registration successful");
    successResponse.setData(userResponse);
    successResponse.setTokens(tokenResponse);
  }

  @Test
  void register_ValidRequest_ReturnsCreated() throws Exception {
    when(userRegistrationService.registerUser(any(CreateUserRequest.class)))
        .thenReturn(successResponse);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Registration successful"))
        .andExpect(jsonPath("$.data.email").value("test@example.com"))
        .andExpect(jsonPath("$.data.firstName").value("John"))
        .andExpect(jsonPath("$.data.lastName").value("Doe"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.emailVerified").value(false))
        .andExpect(jsonPath("$.tokens.accessToken").value("access-token"))
        .andExpect(jsonPath("$.tokens.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.tokens.tokenType").value("bearer"))
        .andExpect(jsonPath("$.tokens.expiresIn").value(900));
  }

  @Test
  void register_DuplicateEmail_ReturnsConflict() throws Exception {
    when(userRegistrationService.registerUser(any(CreateUserRequest.class)))
        .thenThrow(new UserAlreadyExistsException("Email already exists"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors.field").value("email"))
        .andExpect(jsonPath("$.errors.message").value("Email already exists"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void register_InvalidEmail_ReturnsBadRequest() throws Exception {
    validRequest.setEmail("invalid-email");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void register_EmptyEmail_ReturnsBadRequest() throws Exception {
    validRequest.setEmail("");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void register_WeakPassword_ReturnsBadRequest() throws Exception {
    validRequest.setPassword("weak");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void register_EmptyFirstName_ReturnsBadRequest() throws Exception {
    validRequest.setFirstName("");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void register_EmptyLastName_ReturnsBadRequest() throws Exception {
    validRequest.setLastName("");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void register_NullRequest_ReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
