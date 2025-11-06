package com.ecsite.auth.repository;

import com.ecsite.auth.entity.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * パスワードリセットトークンリポジトリ
 *
 * <p>パスワードリセットトークンのデータベース操作を提供します。
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  /**
   * トークン文字列でパスワードリセットトークンを検索します。
   *
   * @param token トークン文字列
   * @return パスワードリセットトークン（存在しない場合はEmpty）
   */
  Optional<PasswordResetToken> findByToken(String token);
}
