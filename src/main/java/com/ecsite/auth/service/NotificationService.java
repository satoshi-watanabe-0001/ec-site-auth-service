package com.ecsite.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知サービス
 *
 * <p>Notification Serviceとの連携を提供します。 現在はログ出力のみで、実際のNotification Service連携は今後実装予定です。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  /**
   * メール認証用のメールを送信します。
   *
   * <p>現在はログ出力のみで、実際のNotification Service連携は今後実装予定です。
   *
   * @param email 送信先メールアドレス
   * @param token 認証トークン
   */
  public void sendVerificationEmail(String email, String token) {
    log.info("Sending verification email to: {}", email);
    log.info("Verification token: {}", token);
    log.info("Verification URL: http://localhost:8081/api/v1/auth/verify-email?token={}", token);
  }

  /**
   * パスワードリセット用のメールを送信します。
   *
   * <p>現在はログ出力のみで、実際のNotification Service連携は今後実装予定です。
   *
   * @param email 送信先メールアドレス
   * @param token パスワードリセットトークン
   */
  public void sendPasswordResetEmail(String email, String token) {
    log.info("Sending password reset email to: {}", email);
    log.info("Password reset token: {}", token);
    log.info(
        "Password reset URL: http://localhost:8081/api/v1/auth/reset-password?token={}", token);
  }

  /**
   * 退会確認用のメールを送信します。
   *
   * <p>現在はログ出力のみで、実際のNotification Service連携は今後実装予定です。
   *
   * @param email 送信先メールアドレス
   * @param scheduledDeletionAt 削除予定日時
   */
  public void sendWithdrawalConfirmation(
      String email, java.time.LocalDateTime scheduledDeletionAt) {
    log.info("Sending withdrawal confirmation email to: {}", email);
    log.info("Account will be permanently deleted at: {}", scheduledDeletionAt);
    log.info(
        "If you did not request this, please contact support immediately at support@example.com");
  }
}
