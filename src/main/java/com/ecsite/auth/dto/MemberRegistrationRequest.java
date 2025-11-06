package com.ecsite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会員登録APIのリクエストDTO
 *
 * <p>EC-11チケット仕様に基づく会員登録リクエストの形式を定義します。 チケット仕様で指定された{name, description, status}形式を実装しています。
 *
 * @see com.ecsite.auth.controller.AuthController#registerMember(MemberRegistrationRequest)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRegistrationRequest {

  /**
   * 会員名
   *
   * <p>会員の識別名または表示名として使用されます。
   */
  @NotBlank(message = "Name is required")
  @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
  private String name;

  /**
   * 会員の説明
   *
   * <p>会員に関する追加情報や説明文を格納します。
   */
  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  /**
   * 会員ステータス
   *
   * <p>会員の初期ステータスを指定します。 有効な値: PENDING, ACTIVE, INACTIVE, SUSPENDED
   */
  @NotBlank(message = "Status is required")
  @Size(max = 20, message = "Status must not exceed 20 characters")
  private String status;
}
