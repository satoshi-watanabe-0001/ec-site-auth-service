package com.ecsite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * メール認証レスポンスDTO
 *
 * <p>メールアドレス認証の結果を返します。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationResponse {

  /** 処理ステータス（success/error） */
  private String status;

  /** メッセージ */
  private String message;

  /** ユーザー情報 */
  private UserResponse data;
}
