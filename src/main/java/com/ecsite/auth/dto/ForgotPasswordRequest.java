package com.ecsite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * パスワードリセットリクエストDTO
 *
 * <p>パスワードを忘れたユーザーがリセットトークンを要求する際のリクエストデータ。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;
}
