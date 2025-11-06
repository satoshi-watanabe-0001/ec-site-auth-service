package com.ecsite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * パスワードリセットレスポンスDTO
 *
 * <p>パスワードリセット操作の結果を返すレスポンスデータ。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {

  private String status;
  private String message;
}
