package com.ecsite.auth.service;

import com.ecsite.auth.dto.LoginRequest;
import com.ecsite.auth.dto.LoginResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.UserRepository;
import com.ecsite.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザーログイン認証サービス
 *
 * <p>メール/パスワード認証によるユーザーログイン処理を担当する。 認証成功時にJWTトークン（アクセストークン + リフレッシュトークン）を発行する。
 *
 * <p>セキュリティ考慮事項:
 *
 * <ul>
 *   <li>パスワード検証にBCryptを使用（strength 12）
 *   <li>ユーザー列挙攻撃対策として汎用エラーメッセージを返す
 *   <li>ACTIVEステータスのユーザーのみログイン許可
 * </ul>
 *
 * @since 1.0
 * @see LoginRequest
 * @see LoginResponse
 * @see JwtUtil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  /**
   * ユーザーを認証し、JWTトークンを発行する
   *
   * <p>以下の処理を順次実行:
   *
   * <ol>
   *   <li>メールアドレスでユーザーを検索
   *   <li>パスワードをBCryptで検証
   *   <li>ユーザーステータスがACTIVEであることを確認
   *   <li>アクセストークンとリフレッシュトークンを生成
   * </ol>
   *
   * <p>セキュリティ上の理由により、ユーザー未発見とパスワード不一致は 同じエラーメッセージ "Invalid email or password" を返す。
   * これによりユーザー列挙攻撃を防ぐ。
   *
   * @param request ログインリクエスト（メール、パスワード、rememberMeフラグ）。nullは許可されない。
   * @return JWTトークンとユーザー情報を含む {@link LoginResponse}
   * @throws BadCredentialsException ユーザーが存在しない、パスワードが不正、 またはユーザーステータスがACTIVEでない場合
   * @since 1.0
   */
  @Transactional(readOnly = true)
  public LoginResponse authenticateUser(LoginRequest request) {
    log.info("Authentication attempt for email: {}", request.getEmail());

    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(
                () -> {
                  log.warn("Authentication failed: User not found - {}", request.getEmail());
                  return new BadCredentialsException("Invalid email or password");
                });

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      log.warn("Authentication failed: Invalid password for user - {}", request.getEmail());
      throw new BadCredentialsException("Invalid email or password");
    }

    if (!user.isActive()) {
      log.warn("Authentication failed: User is not active - {}", request.getEmail());
      throw new BadCredentialsException(
          "Account is not active. Please verify your email or contact support.");
    }

    log.info("Authentication successful for user: {}", user.getId());

    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");
    String refreshToken = jwtUtil.generateRefreshToken(user.getId());

    LoginResponse.UserInfo userInfo =
        LoginResponse.UserInfo.builder()
            .id(user.getId().toString())
            .email(user.getEmail())
            .roles(new String[] {"user"})
            .mfaEnabled(false)
            .build();

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("bearer")
        .expiresIn(900)
        .user(userInfo)
        .build();
  }
}
