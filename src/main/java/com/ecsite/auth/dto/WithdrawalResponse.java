package com.ecsite.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー退会処理のレスポンスDTO
 *
 * <p>退会処理の受付結果を返します。 猶予期間と削除予定日時を含みます。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {

  private String status;
  private String message;
  private WithdrawalData data;

  /** 退会処理の詳細データ */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WithdrawalData {

    private UUID userId;
    private String userStatus;
    private LocalDateTime scheduledDeletionAt;
    private Integer gracePeriodDays;
  }
}
