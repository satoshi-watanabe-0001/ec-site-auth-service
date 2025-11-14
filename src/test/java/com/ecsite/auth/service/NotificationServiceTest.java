package com.ecsite.auth.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * NotificationServiceのユニットテスト
 *
 * <p>通知サービスのログ出力を検証します。
 */
class NotificationServiceTest {

  private NotificationService notificationService;
  private Logger logger;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationService();
    logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(listAppender);
  }

  @Test
  void sendVerificationEmail_LogsCorrectly() {
    String email = "test@example.com";
    String token = "verification-token-123";

    notificationService.sendVerificationEmail(email, token);

    assertLogContains("Sending verification email to: " + email);
    assertLogContains("Verification token: " + token);
    assertLogContains(
        "Verification URL: http://localhost:8081/api/v1/auth/verify-email?token=" + token);
  }

  @Test
  void sendPasswordResetEmail_LogsCorrectly() {
    String email = "test@example.com";
    String token = "reset-token-456";

    notificationService.sendPasswordResetEmail(email, token);

    assertLogContains("Sending password reset email to: " + email);
    assertLogContains("Password reset token: " + token);
    assertLogContains(
        "Password reset URL: http://localhost:8081/api/v1/auth/reset-password?token=" + token);
  }

  @Test
  void sendWithdrawalConfirmation_LogsCorrectly() {
    String email = "test@example.com";
    LocalDateTime scheduledDeletionAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

    notificationService.sendWithdrawalConfirmation(email, scheduledDeletionAt);

    assertLogContains("Sending withdrawal confirmation email to: " + email);
    assertLogContains("Account will be permanently deleted at: " + scheduledDeletionAt.toString());
    assertLogContains(
        "If you did not request this, please contact support immediately at support@example.com");
  }

  private void assertLogContains(String expectedMessage) {
    boolean found =
        listAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(expectedMessage));
    if (!found) {
      throw new AssertionError("Expected log message not found: " + expectedMessage);
    }
  }
}
