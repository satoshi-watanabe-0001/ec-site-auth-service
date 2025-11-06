package com.ecsite.auth.service;

import com.ecsite.auth.entity.PasswordResetToken;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.PasswordResetTokenRepository;
import com.ecsite.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * パスワードリセットサービス
 *
 * <p>パスワードリセットトークンの生成、検証、およびパスワード更新処理を提供します。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final BCryptPasswordEncoder passwordEncoder;

  private static final int TOKEN_EXPIRATION_HOURS = 24;

  /**
   * パスワードリセットトークンを生成し、通知を送信します。
   *
   * <p>セキュリティ上の理由から、メールアドレスが存在しない場合でも同じメッセージを返します。
   *
   * @param email ユーザーのメールアドレス
   * @return 常に成功メッセージを返す
   */
  @Transactional
  public String generatePasswordResetToken(String email) {
    log.info("Password reset requested for email: {}", email);

    userRepository
        .findByEmail(email)
        .ifPresent(
            user -> {
              String tokenValue = UUID.randomUUID().toString();
              LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

              PasswordResetToken token =
                  PasswordResetToken.builder()
                      .token(tokenValue)
                      .user(user)
                      .expiresAt(expiresAt)
                      .build();

              tokenRepository.save(token);
              log.info("Password reset token saved for user: {}", user.getId());

              notificationService.sendPasswordResetEmail(user.getEmail(), tokenValue);
              log.info("Password reset email notification sent for user: {}", user.getId());
            });

    return "If the email exists, a password reset link has been sent";
  }

  /**
   * パスワードリセットトークンを検証し、新しいパスワードを設定します。
   *
   * @param tokenValue トークン文字列
   * @param newPassword 新しいパスワード
   * @return リセットが成功した場合true
   * @throws IllegalArgumentException トークンが無効な場合
   */
  @Transactional
  public boolean resetPassword(String tokenValue, String newPassword) {
    log.info("Password reset attempt with token: {}", tokenValue);

    PasswordResetToken token =
        tokenRepository
            .findByToken(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

    if (!token.isValid()) {
      log.warn("Token is invalid (expired or already used): {}", tokenValue);
      throw new IllegalArgumentException("Token is expired or already used");
    }

    User user = token.getUser();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);

    log.info("Password reset completed for user: {}", user.getId());
    return true;
  }
}
