package com.ecsite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインレスポンスDTO
 *
 * <p>ログイン成功時のレスポンスボディを表現する。 JWTトークン（アクセストークン、リフレッシュトークン）とユーザー情報を含む。
 *
 * <p>レスポンス構造:
 *
 * <ul>
 *   <li>accessToken: アクセストークン（有効期限15分）
 *   <li>refreshToken: リフレッシュトークン（有効期限30日）
 *   <li>tokenType: トークンタイプ（"bearer"）
 *   <li>expiresIn: アクセストークン有効期限（秒単位、900秒=15分）
 *   <li>user: ユーザー情報（ID、メール、ロール、MFA有効化状態）
 * </ul>
 *
 * @since 1.0
 * @see LoginRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private long expiresIn;
  private UserInfo user;

  /**
   * ユーザー情報DTO
   *
   * <p>ログインレスポンスに含まれるユーザー基本情報を表現する。 認証後のクライアントアプリケーションで使用される最小限のユーザー情報を含む。
   *
   * @since 1.0
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private String id;
    private String email;
    private String[] roles;
    private boolean mfaEnabled;
  }
}
