package com.ecsite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインリクエストDTO
 *
 * <p>ユーザーログイン時のリクエストボディを表現する。 メールアドレスとパスワードによる認証に必要な情報を含む。
 *
 * <p>バリデーション:
 *
 * <ul>
 *   <li>email: 必須、メール形式
 *   <li>password: 必須
 *   <li>rememberMe: 任意（デフォルト: false）
 * </ul>
 *
 * @since 1.0
 * @see LoginResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  @NotBlank(message = "Password is required")
  private String password;

  @Builder.Default private boolean rememberMe = false;
}
