package com.ecsite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * メール認証リクエストDTO
 *
 * <p>メールアドレス認証のためのトークンを受け取ります。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequest {

  /** メール認証トークン */
  @NotBlank(message = "Token is required")
  private String token;
}
