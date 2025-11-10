package com.ecsite.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecsite.auth.config.SecurityConfig;
import com.ecsite.auth.dto.WithdrawalRequest;
import com.ecsite.auth.dto.WithdrawalResponse;
import com.ecsite.auth.service.WithdrawalService;
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

/**
 * WithdrawalControllerのユニットテスト
 *
 * <p>ユーザー退会処理コントローラーのAPIエンドポイントをテストします。
 */
@WebMvcTest(WithdrawalController.class)
@Import(SecurityConfig.class)
class WithdrawalControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private WithdrawalService withdrawalService;

  private UUID userId;
  private WithdrawalRequest withdrawalRequest;
  private WithdrawalResponse withdrawalResponse;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    withdrawalRequest = new WithdrawalRequest();
    withdrawalRequest.setReason("サービスを利用しなくなったため");

    WithdrawalResponse.WithdrawalData data =
        WithdrawalResponse.WithdrawalData.builder()
            .userId(userId)
            .userStatus("PENDING_DELETION")
            .scheduledDeletionAt(LocalDateTime.now().plusDays(30))
            .gracePeriodDays(30)
            .build();

    withdrawalResponse =
        WithdrawalResponse.builder().status("success").message("退会処理を受け付けました").data(data).build();
  }

  @Test
  void withdrawUser_Success() throws Exception {
    when(withdrawalService.withdrawUser(eq(userId), any(WithdrawalRequest.class)))
        .thenReturn(withdrawalResponse);

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalRequest))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("退会処理を受け付けました"))
        .andExpect(jsonPath("$.data.userId").value(userId.toString()))
        .andExpect(jsonPath("$.data.userStatus").value("PENDING_DELETION"))
        .andExpect(jsonPath("$.data.scheduledDeletionAt").exists())
        .andExpect(jsonPath("$.data.gracePeriodDays").value(30));
  }

  @Test
  void withdrawUser_WithoutReason_Success() throws Exception {
    WithdrawalRequest requestWithoutReason = new WithdrawalRequest();
    requestWithoutReason.setReason(null);

    when(withdrawalService.withdrawUser(eq(userId), any(WithdrawalRequest.class)))
        .thenReturn(withdrawalResponse);

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutReason))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void withdrawUser_UserNotFound_ReturnsNotFound() throws Exception {
    when(withdrawalService.withdrawUser(eq(userId), any(WithdrawalRequest.class)))
        .thenThrow(new IllegalArgumentException("User not found with ID: " + userId));

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalRequest))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User not found with ID: " + userId))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void withdrawUser_AlreadyPendingDeletion_ReturnsConflict() throws Exception {
    when(withdrawalService.withdrawUser(eq(userId), any(WithdrawalRequest.class)))
        .thenThrow(new IllegalStateException("User is already pending deletion"));

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalRequest))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User is already pending deletion"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void withdrawUser_AlreadyDeleted_ReturnsConflict() throws Exception {
    when(withdrawalService.withdrawUser(eq(userId), any(WithdrawalRequest.class)))
        .thenThrow(new IllegalStateException("User is already deleted"));

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalRequest))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User is already deleted"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void withdrawUser_ReasonTooLong_ReturnsBadRequest() throws Exception {
    WithdrawalRequest invalidRequest = new WithdrawalRequest();
    invalidRequest.setReason("a".repeat(1001)); // 1001文字（制限は1000文字）

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void withdrawUser_InvalidUUID_ReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalRequest))
                .with(csrf())
                .with(user("test@example.com").roles("USER")))
        .andExpect(status().isNotFound());
  }
}
