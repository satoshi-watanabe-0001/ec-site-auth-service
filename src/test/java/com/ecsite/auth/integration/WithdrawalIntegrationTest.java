package com.ecsite.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecsite.auth.dto.WithdrawalRequest;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.entity.User.UserStatus;
import com.ecsite.auth.repository.UserRepository;
import com.ecsite.auth.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * WithdrawalController統合テスト
 *
 * <p>SpringBootTest + Testcontainersを使用して、実際のデータベースとの統合をテストします。
 *
 * <p>テスト内容:
 *
 * <ul>
 *   <li>JWT認証フローの検証
 *   <li>Flywayマイグレーション（V3）の適用確認
 *   <li>データベース状態の変更確認（PENDING_DELETION、deletion_scheduled_at）
 *   <li>認可チェック（自己退会のみ許可、他人はForbidden）
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@TestPropertySource(properties = {"withdrawal.grace-days=30"})
class WithdrawalIntegrationTest {

  @Container
  private static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("auth_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private JwtUtil jwtUtil;

  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;
  private String jwtToken;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setEmail("integration-test@example.com");
    testUser.setPasswordHash(passwordEncoder.encode("SecurePass123!"));
    testUser.setFirstName("Integration");
    testUser.setLastName("Test");
    testUser.setStatus(UserStatus.ACTIVE);
    testUser.setEmailVerifiedAt(LocalDateTime.now());
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());
    testUser = userRepository.save(testUser);

    jwtToken = jwtUtil.generateAccessToken(testUser.getId(), testUser.getEmail(), "USER");
  }

  @Test
  void withdrawUser_WithValidJWT_Success() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", testUser.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("退会処理を受け付けました"))
        .andExpect(jsonPath("$.data.userId").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.data.userStatus").value("PENDING_DELETION"))
        .andExpect(jsonPath("$.data.scheduledDeletionAt").exists())
        .andExpect(jsonPath("$.data.gracePeriodDays").value(30));

    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getStatus()).isEqualTo(UserStatus.PENDING_DELETION);
    assertThat(updatedUser.get().getDeletionScheduledAt()).isNotNull();
    assertThat(updatedUser.get().getWithdrawalReason()).isEqualTo("統合テストのため");
  }

  @Test
  void withdrawUser_WithoutJWT_ReturnsForbidden() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void withdrawUser_WithInvalidJWT_ReturnsForbidden() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", testUser.getId())
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void withdrawUser_DifferentUser_ReturnsForbidden() throws Exception {
    User anotherUser = new User();
    anotherUser.setEmail("another-user@example.com");
    anotherUser.setPasswordHash(passwordEncoder.encode("SecurePass123!"));
    anotherUser.setFirstName("Another");
    anotherUser.setLastName("User");
    anotherUser.setStatus(UserStatus.ACTIVE);
    anotherUser.setEmailVerifiedAt(LocalDateTime.now());
    anotherUser.setCreatedAt(LocalDateTime.now());
    anotherUser.setUpdatedAt(LocalDateTime.now());
    anotherUser = userRepository.save(anotherUser);

    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", anotherUser.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("自分自身のアカウントのみ退会できます"));

    Optional<User> unchangedUser = userRepository.findById(anotherUser.getId());
    assertThat(unchangedUser).isPresent();
    assertThat(unchangedUser.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(unchangedUser.get().getDeletionScheduledAt()).isNull();
  }

  @Test
  void withdrawUser_NonExistentUser_ReturnsNotFound() throws Exception {
    UUID nonExistentUserId = UUID.randomUUID();
    String nonExistentUserToken =
        jwtUtil.generateAccessToken(nonExistentUserId, "nonexistent@example.com", "USER");

    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", nonExistentUserId)
                .header("Authorization", "Bearer " + nonExistentUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User not found with ID: " + nonExistentUserId));
  }

  @Test
  void withdrawUser_AlreadyPendingDeletion_ReturnsConflict() throws Exception {
    testUser.setStatus(UserStatus.PENDING_DELETION);
    testUser.setDeletionScheduledAt(LocalDateTime.now().plusDays(30));
    userRepository.save(testUser);

    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", testUser.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User is already pending deletion"));
  }

  @Test
  void withdrawUser_AlreadyDeleted_ReturnsConflict() throws Exception {
    testUser.setStatus(UserStatus.DELETED);
    testUser.setDeletionScheduledAt(LocalDateTime.now().minusDays(1));
    testUser.setDeletedAt(LocalDateTime.now());
    userRepository.save(testUser);

    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("統合テストのため");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", testUser.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User is already deleted"));
  }

  @Test
  void withdrawUser_FlywayMigrationV3Applied_Success() throws Exception {

    WithdrawalRequest request = new WithdrawalRequest();
    request.setReason("Flywayマイグレーション確認テスト");

    mockMvc
        .perform(
            post("/api/v1/users/{id}/withdraw", testUser.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isAccepted());

    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getDeletionScheduledAt()).isNotNull();
    assertThat(updatedUser.get().getWithdrawalReason()).isEqualTo("Flywayマイグレーション確認テスト");
    assertThat(updatedUser.get().getDeletedAt()).isNull();
  }
}
