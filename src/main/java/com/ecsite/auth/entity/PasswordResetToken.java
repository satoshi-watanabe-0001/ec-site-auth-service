package com.ecsite.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * パスワードリセットトークンエンティティ
 *
 * <p>ユーザーのパスワードリセットに使用するトークンを管理します。 トークンは一定期間（デフォルト24時間）有効で、使用後は無効化されます。
 */
@Entity
@Table(name = "password_reset_tokens", schema = "auth_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "token", nullable = false, unique = true, length = 255)
  private String token;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * トークンが有効期限内かどうかを判定します。
   *
   * @return 有効期限内の場合true
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  /**
   * トークンが既に使用済みかどうかを判定します。
   *
   * @return 使用済みの場合true
   */
  public boolean isUsed() {
    return usedAt != null;
  }

  /**
   * トークンが有効（未使用かつ有効期限内）かどうかを判定します。
   *
   * @return 有効な場合true
   */
  public boolean isValid() {
    return !isUsed() && !isExpired();
  }
}
