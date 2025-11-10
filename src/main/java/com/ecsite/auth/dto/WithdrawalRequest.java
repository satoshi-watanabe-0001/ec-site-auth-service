package com.ecsite.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー退会処理のリクエストDTO
 *
 * <p>ユーザーがアカウント退会を申請する際に使用されます。 退会理由は任意ですが、サービス改善のために収集されます。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {

  @Size(max = 1000, message = "Withdrawal reason must not exceed 1000 characters")
  private String reason;
}
