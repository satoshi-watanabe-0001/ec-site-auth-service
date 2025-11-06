package com.ecsite.auth.service;

import com.ecsite.auth.entity.EmailVerificationToken;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.EmailVerificationTokenRepository;
import com.ecsite.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * メール認証サービス
 *
 * <p>メールアドレス認証トークンの生成、検証、および認証処理を提供します。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

  private final EmailVerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  private static final int TOKEN_EXPIRATION_HOURS = 24;

  /**
   * メール認証トークンを生成し、通知を送信します。
   *
   * @param user 対象ユーザー
   * @return 生成されたトークン文字列
   */
  @Transactional
  public String generateVerificationToken(User user) {
    log.info("Generating email verification token for user: {}", user.getId());

    String tokenValue = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

    EmailVerificationToken token =
        EmailVerificationToken.builder().token(tokenValue).user(user).expiresAt(expiresAt).build();

    tokenRepository.save(token);
    log.info("Email verification token saved for user: {}", user.getId());

    notificationService.sendVerificationEmail(user.getEmail(), tokenValue);
    log.info("Verification email notification sent for user: {}", user.getId());

    return tokenValue;
  }

  /**
   * メール認証トークンを検証し、ユーザーのメールアドレスを認証済みにします。
   *
   * @param tokenValue トークン文字列
   * @return 認証が成功した場合true
   * @throws IllegalArgumentException トークンが無効な場合
   */
  @Transactional
  public boolean verifyEmail(String tokenValue) {
    log.info("Verifying email with token: {}", tokenValue);

    EmailVerificationToken token =
        tokenRepository
            .findByToken(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

    if (!token.isValid()) {
      log.warn("Token is invalid (expired or already used): {}", tokenValue);
      throw new IllegalArgumentException("Token is expired or already used");
    }

    User user = token.getUser();
    user.setEmailVerifiedAt(LocalDateTime.now());

    if (user.getStatus() == User.UserStatus.PENDING) {
      user.setStatus(User.UserStatus.ACTIVE);
      log.info("User status updated to ACTIVE: {}", user.getId());
    }

    userRepository.save(user);

    token.setVerifiedAt(LocalDateTime.now());
    tokenRepository.save(token);

    log.info("Email verification completed for user: {}", user.getId());
    return true;
  }

  /**
   * メール認証トークンを再送信します。
   *
   * @param email ユーザーのメールアドレス
   * @return 新しいトークン文字列
   * @throws IllegalArgumentException ユーザーが存在しない場合
   */
  @Transactional
  public String resendVerificationToken(String email) {
    log.info("Resending verification token for email: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.isEmailVerified()) {
      log.warn("Email already verified for user: {}", user.getId());
      throw new IllegalArgumentException("Email is already verified");
    }

    return generateVerificationToken(user);
  }
}
